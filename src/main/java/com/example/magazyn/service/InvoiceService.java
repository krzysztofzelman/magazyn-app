package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.InvoiceItemResponse;
import com.example.magazyn.dto.InvoiceResponse;
import com.example.magazyn.entity.*;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvoiceService {

    private static final PDFont FONT_REG = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_SMALL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float MARGIN = 40f;
    private static final float A4_W = PDRectangle.A4.getWidth();
    private static final float A4_H = PDRectangle.A4.getHeight();

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final WarehouseDocumentRepository documentRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final ContractorRepository contractorRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceItemRepository invoiceItemRepository,
                          WarehouseDocumentRepository documentRepository,
                          CompanySettingsRepository companySettingsRepository,
                          ContractorRepository contractorRepository,
                          ProductRepository productRepository,
                          AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.documentRepository = documentRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.contractorRepository = contractorRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Generate an invoice from a confirmed WZ document.
     */
    public InvoiceResponse generateFromDocument(Long documentId, String username) {
        WarehouseDocument doc = documentRepository.findByIdWithItems(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocument", documentId));

        if (doc.getType() != DocumentType.WZ) {
            throw new InvalidOperationException("Faktury można wystawiać tylko dla dokumentów WZ");
        }
        if (doc.getStatus() != DocumentStatus.CONFIRMED) {
            throw new InvalidOperationException("Fakturę można wystawić tylko dla zatwierdzonych dokumentów");
        }

        // Check if invoice already exists for this document
        if (invoiceRepository.findByDocumentId(documentId).isPresent()) {
            throw new InvalidOperationException("Faktura dla tego dokumentu już istnieje");
        }

        Long tenantId = TenantContext.getTenantId();

        // Get company settings (seller)
        CompanySettings seller = companySettingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Brak danych firmy. Skonfiguruj ustawienia firmy."));

        // Get contractor (buyer)
        Contractor buyer = doc.getContractor();

        // Generate invoice number
        String number = generateInvoiceNumber();

        // Build invoice
        Invoice invoice = Invoice.builder()
                .number(number)
                .documentId(documentId)
                .status(InvoiceStatus.ISSUED)
                .sellerName(seller.getName())
                .sellerTaxId(seller.getTaxId())
                .sellerAddress(seller.getAddress())
                .sellerBankAccount(seller.getBankAccount())
                .buyerName(buyer.getName())
                .buyerTaxId(buyer.getTaxId())
                .buyerAddress(buyer.getAddress())
                .issueDate(LocalDate.now())
                .saleDate(doc.getConfirmedAt() != null ? doc.getConfirmedAt().toLocalDate() : LocalDate.now())
                .dueDate(LocalDate.now().plusDays(
                    buyer.getPaymentDays() != null ? buyer.getPaymentDays() : 14))
                .paymentMethod(buyer.getPaymentMethod() != null ? buyer.getPaymentMethod() : "PRZELEW")
                .paymentAccount(buyer.getBankAccount())
                .createdBy(username)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;

        for (WarehouseDocumentItem docItem : doc.getItems()) {
            Product product = docItem.getProduct();
            BigDecimal vatRate = product.getDefaultVatRate() != null
                    ? product.getDefaultVatRate()
                    : new BigDecimal("23.00");
            BigDecimal qty = BigDecimal.valueOf(docItem.getQuantity());
            BigDecimal itemNet = docItem.getUnitPrice().multiply(qty);
            BigDecimal itemVat = itemNet.multiply(vatRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal itemGross = itemNet.add(itemVat);

            InvoiceItem invItem = InvoiceItem.builder()
                    .invoice(invoice)
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productUnit(product.getUnit())
                    .quantity(docItem.getQuantity())
                    .unitPriceNet(docItem.getUnitPrice())
                    .vatRate(vatRate)
                    .vatAmount(itemVat)
                    .totalNet(itemNet)
                    .totalGross(itemGross)
                    .build();

            invoice.getItems().add(invItem);
            totalNet = totalNet.add(itemNet);
            totalVat = totalVat.add(itemVat);
        }

        invoice.setTotalNet(totalNet);
        invoice.setTotalVat(totalVat);
        invoice.setTotalGross(totalNet.add(totalVat));

        invoice = invoiceRepository.save(invoice);

        auditLogService.log(username, "INVOICE_CREATE", "Invoice", invoice.getId(),
                "number=" + number + " document=" + doc.getNumber()
                + " buyer=" + buyer.getName() + " total=" + invoice.getTotalGross());

        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices(String statusFilter) {
        Long tenantId = TenantContext.getTenantId();
        List<Invoice> invoices;
        if (statusFilter != null && !statusFilter.isBlank()) {
            InvoiceStatus status = InvoiceStatus.valueOf(statusFilter.toUpperCase());
            invoices = invoiceRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        } else {
            invoices = invoiceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }
        return invoices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long id) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        return toResponse(invoice);
    }

    public InvoiceResponse payInvoice(Long id, String paymentMethod, String paymentAccount, String username) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new InvalidOperationException("Tylko wystawione faktury można oznaczyć jako opłacone");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        if (paymentMethod != null) invoice.setPaymentMethod(paymentMethod);
        if (paymentAccount != null) invoice.setPaymentAccount(paymentAccount);
        invoice = invoiceRepository.save(invoice);

        auditLogService.log(username, "INVOICE_PAY", "Invoice", id,
                "number=" + invoice.getNumber());

        return toResponse(invoice);
    }

    public InvoiceResponse cancelInvoice(Long id, String username) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidOperationException("Faktura jest już anulowana");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidOperationException("Nie można anulować opłaconej faktury");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancelledAt(LocalDateTime.now());
        invoice = invoiceRepository.save(invoice);

        auditLogService.log(username, "INVOICE_CANCEL", "Invoice", id,
                "number=" + invoice.getNumber());

        return toResponse(invoice);
    }

    public byte[] exportInvoicePdf(Long id) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        // Load items explicitly
        if (invoice.getItems().isEmpty()) {
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(id);
            invoice.setItems(items);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = A4_H - MARGIN;
                float col1 = MARGIN;
                float col2 = A4_W / 2;
                float rowH = 14f;

                // Title
                cs.setFont(FONT_BOLD, 18);
                y = drawText(cs, "FAKTURA VAT", col1, y, 0);
                cs.setFont(FONT_REG, 10);
                y = drawText(cs, "Nr " + invoice.getNumber(), col1, y, 0);

                // Seller / Buyer info boxes
                y -= 10;

                // Seller box
                float boxY = y;
                float boxW = (A4_W - 2 * MARGIN) / 2 - 5;
                cs.setFont(FONT_BOLD, 9);
                drawText(cs, "SPRZEDAWCA:", col1, y, 0);
                cs.setFont(FONT_REG, 9);
                y -= 12;
                y = drawText(cs, invoice.getSellerName(), col1, y, 0);
                if (invoice.getSellerTaxId() != null)
                    y = drawText(cs, "NIP: " + invoice.getSellerTaxId(), col1, y, 0);
                if (invoice.getSellerAddress() != null)
                    y = drawText(cs, invoice.getSellerAddress(), col1, y, 0);
                if (invoice.getSellerBankAccount() != null)
                    y = drawText(cs, "Konto: " + invoice.getSellerBankAccount(), col1, y, 0);

                // Buyer box
                float buyerY = boxY;
                cs.setFont(FONT_BOLD, 9);
                drawText(cs, "NABYWCA:", col2, buyerY, 0);
                cs.setFont(FONT_REG, 9);
                buyerY -= 12;
                buyerY = drawText(cs, invoice.getBuyerName(), col2, buyerY, 0);
                if (invoice.getBuyerTaxId() != null)
                    buyerY = drawText(cs, "NIP: " + invoice.getBuyerTaxId(), col2, buyerY, 0);
                if (invoice.getBuyerAddress() != null)
                    buyerY = drawText(cs, invoice.getBuyerAddress(), col2, buyerY, 0);

                y = Math.min(y, buyerY) - 10;

                // Date / Payment info
                cs.setFont(FONT_REG, 9);
                y = drawText(cs, "Data wystawienia: " + invoice.getIssueDate(), col1, y, 0);
                y = drawText(cs, "Data sprzedaży: " + invoice.getSaleDate(), col1, y, 0);
                y = drawText(cs, "Termin płatności: " + invoice.getDueDate(), col1, y, 0);
                y = drawText(cs, "Metoda płatności: " + invoice.getPaymentMethod(), col1, y, 0);
                if (invoice.getPaymentAccount() != null)
                    y = drawText(cs, "Nr konta: " + invoice.getPaymentAccount(), col1, y, 0);

                y -= 8;

                // Table header
                float tableTop = y;
                float[] colWidths = {30, 180, 50, 60, 60, 60, 70}; // Lp, Product, Qty, Net price, Net value, VAT%, Gross
                float[] colStarts = new float[colWidths.length];
                colStarts[0] = MARGIN;
                for (int i = 1; i < colWidths.length; i++) {
                    colStarts[i] = colStarts[i - 1] + colWidths[i - 1];
                }
                float tableW = colStarts[colStarts.length - 1] + colWidths[colWidths.length - 1] - MARGIN;

                cs.setFont(FONT_BOLD, 8);
                y = drawTableRow(cs, colStarts, colWidths, y, new String[]{
                        "Lp.", "Produkt", "Ilo\u015B\u0107", "Cena netto", "Warto\u015B\u0107 netto", "VAT %", "Warto\u015B\u0107 brutto"
                });
                // Table header underline
                cs.setStrokingColor(0.6f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(MARGIN + tableW, y);
                cs.stroke();
                y -= 4;

                // Table rows
                cs.setFont(FONT_REG, 8);
                int lp = 1;
                for (InvoiceItem item : invoice.getItems()) {
                    if (y < 60) {
                        // Page overflow — simple: just stop, draw total on next page not implemented for brevity
                        break;
                    }
                    String qtyStr = item.getQuantity() + " " + (item.getProductUnit() != null ? item.getProductUnit() : "szt");
                    y = drawTableRow(cs, colStarts, colWidths, y, new String[]{
                            String.valueOf(lp++),
                            item.getProductName(),
                            qtyStr,
                            formatPrice(item.getUnitPriceNet()),
                            formatPrice(item.getTotalNet()),
                            formatVat(item.getVatRate()),
                            formatPrice(item.getTotalGross())
                    });
                    y -= 2;
                }

                // Bottom line
                cs.setStrokingColor(0.6f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(MARGIN + tableW, y);
                cs.stroke();
                y -= 6;

                // Totals
                cs.setFont(FONT_BOLD, 10);
                y = drawTextRight(cs, "Razem netto: " + formatPrice(invoice.getTotalNet()),
                        MARGIN + tableW, y, 0);
                y = drawTextRight(cs, "W tym VAT: " + formatPrice(invoice.getTotalVat()),
                        MARGIN + tableW, y, 0);
                cs.setFont(FONT_BOLD, 12);
                y = drawTextRight(cs, "RAZEM BRUTTO: " + formatPrice(invoice.getTotalGross()),
                        MARGIN + tableW, y, 0);

                // Status stamp
                if (invoice.getStatus() == InvoiceStatus.PAID) {
                    y -= 20;
                    cs.setFont(FONT_BOLD, 14);
                    cs.setNonStrokingColor(0, 0.6f, 0);
                    drawText(cs, "ZAPŁACONO", MARGIN, y, 0);
                    cs.setNonStrokingColor(0, 0, 0);
                } else if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                    y -= 20;
                    cs.setFont(FONT_BOLD, 14);
                    cs.setNonStrokingColor(0.8f, 0, 0);
                    drawText(cs, "ANULOWANO", MARGIN, y, 0);
                    cs.setNonStrokingColor(0, 0, 0);
                }
            }

            doc.save(baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }

        return baos.toByteArray();
    }

    private String generateInvoiceNumber() {
        String prefix = "FV/" + Year.now().getValue() + "/";
        String maxNumber = invoiceRepository.findMaxNumberByPrefix(prefix).orElse(null);

        int nextSeq = 1;
        if (maxNumber != null) {
            String seqPart = maxNumber.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException ignored) {}
        }

        return prefix + String.format("%03d", nextSeq);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        String documentNumber = null;
        if (invoice.getDocumentId() != null) {
            documentNumber = documentRepository.findById(invoice.getDocumentId())
                    .map(WarehouseDocument::getNumber)
                    .orElse(null);
        }

        return new InvoiceResponse(
                invoice.getId(), invoice.getNumber(),
                invoice.getDocumentId(), documentNumber,
                invoice.getStatus(),
                invoice.getSellerName(), invoice.getSellerTaxId(),
                invoice.getSellerAddress(), invoice.getSellerBankAccount(),
                invoice.getBuyerName(), invoice.getBuyerTaxId(), invoice.getBuyerAddress(),
                invoice.getIssueDate(), invoice.getSaleDate(), invoice.getDueDate(),
                invoice.getPaymentMethod(), invoice.getPaymentAccount(),
                invoice.getTotalNet(), invoice.getTotalVat(), invoice.getTotalGross(),
                invoice.getNotes(), invoice.getCreatedBy(),
                invoice.getCreatedAt(), invoice.getPaidAt(), invoice.getCancelledAt(),
                invoice.getItems().stream().map(this::toItemResponse).collect(Collectors.toList())
        );
    }

    private InvoiceItemResponse toItemResponse(InvoiceItem item) {
        return new InvoiceItemResponse(
                item.getId(), item.getProductId(),
                item.getProductName(), item.getProductSku(), item.getProductUnit(),
                item.getQuantity(), item.getUnitPriceNet(),
                item.getVatRate(), item.getVatAmount(),
                item.getTotalNet(), item.getTotalGross()
        );
    }

    // PDF helpers

    private float drawText(PDPageContentStream cs, String text, float x, float y, float extra) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - 14;
    }

    private float drawTextRight(PDPageContentStream cs, String text, float rightX, float y, float extra) throws IOException {
        float textWidth = FONT_BOLD.getStringWidth(text) / 1000f * 10;
        cs.beginText();
        cs.newLineAtOffset(rightX - textWidth, y);
        cs.showText(text);
        cs.endText();
        return y - 16;
    }

    private float drawTableRow(PDPageContentStream cs, float[] colStarts, float[] colWidths,
                                float y, String[] values) throws IOException {
        float maxH = 0;
        for (int i = 0; i < values.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(colStarts[i] + 2, y);
            cs.showText(values[i]);
            cs.endText();
        }
        return y - 10;
    }

    private String formatPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP).toString() + " PLN";
    }

    private String formatVat(BigDecimal vatRate) {
        return vatRate.setScale(0, RoundingMode.HALF_UP).toString() + "%";
    }
}
