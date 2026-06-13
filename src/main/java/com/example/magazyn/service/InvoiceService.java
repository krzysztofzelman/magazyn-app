package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.CreateInvoiceRequest;
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
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvoiceService {

    private static final float MARGIN = 40f;
    private static final float BOTTOM_M = 55f;
    private static final float A4_W = PDRectangle.A4.getWidth();
    private static final float A4_H = PDRectangle.A4.getHeight();

    private static final float[] COL_WIDTHS = {30f, 190f, 50f, 60f, 60f, 55f, 70f};
    private static final float[] COL_STARTS;
    private static final float TABLE_W;
    static {
        COL_STARTS = new float[COL_WIDTHS.length];
        COL_STARTS[0] = MARGIN;
        for (int i = 1; i < COL_WIDTHS.length; i++) {
            COL_STARTS[i] = COL_STARTS[i - 1] + COL_WIDTHS[i - 1];
        }
        TABLE_W = COL_STARTS[COL_STARTS.length - 1] + COL_WIDTHS[COL_WIDTHS.length - 1] - MARGIN;
    }

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
                .warehouseId(doc.getWarehouseId())
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
                    .warehouseId(doc.getWarehouseId())
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

    /**
     * Create a blank DRAFT invoice (not from a WZ document).
     */
    public InvoiceResponse createBlankInvoice(CreateInvoiceRequest request, String username) {
        Long tenantId = TenantContext.getTenantId();
        CompanySettings seller = companySettingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Brak danych firmy. Skonfiguruj ustawienia firmy."));

        String draftNumber = generateDraftNumber();

        Invoice invoice = Invoice.builder()
                .number(draftNumber)
                .status(InvoiceStatus.DRAFT)
                .sellerName(seller.getName())
                .sellerTaxId(seller.getTaxId())
                .sellerAddress(seller.getAddress())
                .sellerBankAccount(seller.getBankAccount())
                .buyerName(request.buyerName())
                .buyerTaxId(request.buyerTaxId())
                .buyerAddress(request.buyerAddress())
                .issueDate(LocalDate.now())
                .saleDate(request.saleDate())
                .dueDate(request.dueDate())
                .paymentMethod(request.paymentMethod() != null ? request.paymentMethod() : "PRZELEW")
                .paymentAccount(request.paymentAccount())
                .notes(request.notes())
                .createdBy(username)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;

        for (var reqItem : request.items()) {
            BigDecimal qty = BigDecimal.valueOf(reqItem.quantity());
            BigDecimal vatRate = reqItem.vatRate();
            BigDecimal itemNet = reqItem.unitPriceNet().multiply(qty);
            BigDecimal itemVat = itemNet.multiply(vatRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal itemGross = itemNet.add(itemVat);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .productId(null)
                    .productName(reqItem.productName())
                    .productSku(reqItem.productSku())
                    .productUnit(reqItem.productUnit())
                    .quantity(reqItem.quantity())
                    .unitPriceNet(reqItem.unitPriceNet())
                    .vatRate(vatRate)
                    .vatAmount(itemVat)
                    .totalNet(itemNet)
                    .totalGross(itemGross)
                    .build();

            invoice.getItems().add(item);
            totalNet = totalNet.add(itemNet);
            totalVat = totalVat.add(itemVat);
        }

        invoice.setTotalNet(totalNet);
        invoice.setTotalVat(totalVat);
        invoice.setTotalGross(totalNet.add(totalVat));

        invoice = invoiceRepository.save(invoice);

        auditLogService.log(username, "INVOICE_DRAFT_CREATE", "Invoice", invoice.getId(),
                "number=" + draftNumber + " buyer=" + request.buyerName() + " total=" + invoice.getTotalGross());

        return toResponse(invoice);
    }

    /**
     * Update a DRAFT invoice — replaces items in full.
     */
    public InvoiceResponse updateInvoice(Long id, CreateInvoiceRequest request, String username) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new InvalidOperationException("Tylko faktury w statusie DRAFT można edytować");
        }

        invoice.setBuyerName(request.buyerName());
        invoice.setBuyerTaxId(request.buyerTaxId());
        invoice.setBuyerAddress(request.buyerAddress());
        invoice.setSaleDate(request.saleDate());
        invoice.setDueDate(request.dueDate());
        if (request.paymentMethod() != null) invoice.setPaymentMethod(request.paymentMethod());
        if (request.paymentAccount() != null) invoice.setPaymentAccount(request.paymentAccount());
        invoice.setNotes(request.notes());

        // Replace items
        invoice.getItems().clear();
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;

        for (var reqItem : request.items()) {
            BigDecimal qty = BigDecimal.valueOf(reqItem.quantity());
            BigDecimal vatRate = reqItem.vatRate();
            BigDecimal itemNet = reqItem.unitPriceNet().multiply(qty);
            BigDecimal itemVat = itemNet.multiply(vatRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal itemGross = itemNet.add(itemVat);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .productId(null)
                    .productName(reqItem.productName())
                    .productSku(reqItem.productSku())
                    .productUnit(reqItem.productUnit())
                    .quantity(reqItem.quantity())
                    .unitPriceNet(reqItem.unitPriceNet())
                    .vatRate(vatRate)
                    .vatAmount(itemVat)
                    .totalNet(itemNet)
                    .totalGross(itemGross)
                    .build();

            invoice.getItems().add(item);
            totalNet = totalNet.add(itemNet);
            totalVat = totalVat.add(itemVat);
        }

        invoice.setTotalNet(totalNet);
        invoice.setTotalVat(totalVat);
        invoice.setTotalGross(totalNet.add(totalVat));

        invoice = invoiceRepository.save(invoice);

        auditLogService.log(username, "INVOICE_DRAFT_UPDATE", "Invoice", id,
                "number=" + invoice.getNumber());

        return toResponse(invoice);
    }

    /**
     * Delete a DRAFT invoice permanently.
     */
    public void deleteInvoice(Long id, String username) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new InvalidOperationException("Tylko faktury w statusie DRAFT można usunąć");
        }

        invoiceRepository.delete(invoice);

        auditLogService.log(username, "INVOICE_DRAFT_DELETE", "Invoice", id,
                "number=" + invoice.getNumber());
    }

    /**
     * Issue a DRAFT invoice — changes status to ISSUED and assigns a proper FV number.
     */
    public InvoiceResponse issueInvoice(Long id, String username) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new InvalidOperationException("Tylko faktury w statusie DRAFT można wystawić");
        }

        String fvNumber = generateInvoiceNumber();
        invoice.setNumber(fvNumber);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(LocalDate.now());

        invoice = invoiceRepository.save(invoice);

        auditLogService.log(username, "INVOICE_ISSUE", "Invoice", id,
                "number=" + fvNumber);

        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices(String statusFilter) {
        return getAllInvoices(statusFilter, null);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices(String statusFilter, Integer year) {
        Long tenantId = TenantContext.getTenantId();
        List<Invoice> invoices;
        boolean hasStatus = statusFilter != null && !statusFilter.isBlank();
        boolean hasYear = year != null && year > 0;

        if (hasStatus && hasYear) {
            InvoiceStatus status = InvoiceStatus.valueOf(statusFilter.toUpperCase());
            invoices = invoiceRepository.findByTenantIdAndStatusAndYear(tenantId, status, year);
        } else if (hasStatus) {
            InvoiceStatus status = InvoiceStatus.valueOf(statusFilter.toUpperCase());
            invoices = invoiceRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        } else if (hasYear) {
            invoices = invoiceRepository.findByTenantIdAndYear(tenantId, year);
        } else {
            invoices = invoiceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }
        return invoices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoicesPaged(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Invoice> page = invoiceRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
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
            PDFont fontReg = loadFont(doc, "/fonts/LiberationSans-Regular.ttf");
            PDFont fontBold = loadFont(doc, "/fonts/LiberationSans-Bold.ttf");

            PdfPageCtx ctx = new PdfPageCtx(doc, invoice, fontReg, fontBold);
            ctx.newPage();

            // === HEADER ===
            ctx.setFont(fontBold, 18);
            ctx.writeLine("FAKTURA VAT", MARGIN);
            ctx.setFont(fontReg, 10);
            ctx.writeLine("Nr " + invoice.getNumber(), MARGIN);
            ctx.y -= 10;

            ctx.startSellerBuyerSection();

            // Seller box
            ctx.setFont(fontBold, 9);
            ctx.writeLine("SPRZEDAWCA:", MARGIN);
            ctx.setFont(fontReg, 9);
            ctx.y -= 2;
            ctx.writeLine(invoice.getSellerName(), MARGIN);
            if (invoice.getSellerTaxId() != null) ctx.writeLine("NIP: " + invoice.getSellerTaxId(), MARGIN);
            if (invoice.getSellerAddress() != null) ctx.writeLine(invoice.getSellerAddress(), MARGIN);
            if (invoice.getSellerBankAccount() != null) ctx.writeLine("Konto: " + invoice.getSellerBankAccount(), MARGIN);

            // Buyer box (right column)
            ctx.setFont(fontBold, 9);
            ctx.writeLine("NABYWCA:", ctx.col2);
            ctx.setFont(fontReg, 9);
            ctx.y -= 2;
            ctx.writeLine(invoice.getBuyerName(), ctx.col2);
            if (invoice.getBuyerTaxId() != null) ctx.writeLine("NIP: " + invoice.getBuyerTaxId(), ctx.col2);
            if (invoice.getBuyerAddress() != null) ctx.writeLine(invoice.getBuyerAddress(), ctx.col2);

            ctx.endSellerBuyerSection();

            // Date / Payment info
            ctx.setFont(fontReg, 9);
            ctx.writeLine("Data wystawienia: " + invoice.getIssueDate(), MARGIN);
            ctx.writeLine("Data sprzedaży: " + invoice.getSaleDate(), MARGIN);
            ctx.writeLine("Termin płatności: " + invoice.getDueDate(), MARGIN);
            ctx.writeLine("Metoda płatności: " + invoice.getPaymentMethod(), MARGIN);
            if (invoice.getPaymentAccount() != null)
                ctx.writeLine("Nr konta: " + invoice.getPaymentAccount(), MARGIN);

            ctx.y -= 6;

            // === TABLE ===
            ctx.drawTableHeader();
            int lp = 1;
            for (InvoiceItem item : invoice.getItems()) {
                ctx.checkFooter(30);
                String qtyStr = item.getQuantity() + " " + (item.getProductUnit() != null ? item.getProductUnit() : "szt");
                ctx.drawTableRow(new String[]{
                        String.valueOf(lp++),
                        item.getProductName(),
                        qtyStr,
                        formatPrice(item.getUnitPriceNet()),
                        formatPrice(item.getTotalNet()),
                        formatVat(item.getVatRate()),
                        formatPrice(item.getTotalGross())
                });
            }

            ctx.drawTableBottomLine();

            // === TOTALS (gray background) ===
            ctx.y -= 4;
            ctx.drawTotals(invoice.getTotalNet(), invoice.getTotalVat(), invoice.getTotalGross());

            // === NOTES ===
            if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
                ctx.checkFooter(40);
                ctx.y -= 4;
                ctx.setFont(fontBold, 9);
                ctx.writeLine("Uwagi:", MARGIN);
                ctx.setFont(fontReg, 9);
                ctx.y -= 2;
                ctx.writeLine(invoice.getNotes(), MARGIN);
            }

            // === STATUS STAMP ===
            ctx.drawStatusStamp();

            // === FINALIZE ===
            ctx.finalizeFooter();
            doc.save(baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }

        return baos.toByteArray();
    }

    // ========== PdfPageCtx inner class ==========

    private static class PdfPageCtx {
        final PDDocument doc;
        final Invoice invoice;
        final PDFont fontReg;
        final PDFont fontBold;

        float y;
        int pageNum;
        private PDPageContentStream cs;
        private PDFont currentFont;
        private float currentFontSize;
        final float col2 = A4_W / 2;

        // Seller/buyer section tracking
        private float sellerBuyerStartY;
        private float sellerBuyerMaxX;

        PdfPageCtx(PDDocument doc, Invoice invoice, PDFont fontReg, PDFont fontBold) {
            this.doc = doc;
            this.invoice = invoice;
            this.fontReg = fontReg;
            this.fontBold = fontBold;
        }

        void newPage() throws IOException {
            // Finalize previous page footer if not first page
            if (pageNum > 0) {
                drawFooter();
                cs.close();
            }
            pageNum++;
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = A4_H - MARGIN;
        }

        void checkFooter(float neededSpace) throws IOException {
            if (y < BOTTOM_M + neededSpace) {
                newPage();
            }
        }

        void finalizeFooter() throws IOException {
            drawFooter();
            cs.close();
        }

        private void drawFooter() throws IOException {
            if (cs == null) return;
            String footerText = invoice.getSellerBankAccount() != null && !invoice.getSellerBankAccount().isBlank()
                    ? "Konto: " + invoice.getSellerBankAccount() + "  |  Strona " + pageNum
                    : "Strona " + pageNum;
            cs.setFont(fontReg, 7);
            float textWidth = fontReg.getStringWidth(footerText) / 1000f * 7;
            float x = (A4_W - textWidth) / 2;
            cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
            cs.beginText();
            cs.newLineAtOffset(x, BOTTOM_M - 30);
            cs.showText(footerText);
            cs.endText();
            cs.setNonStrokingColor(0, 0, 0);
        }

        void setFont(PDFont font, float size) throws IOException {
            currentFont = font;
            currentFontSize = size;
            cs.setFont(font, size);
        }

        void writeLine(String text, float x) throws IOException {
            cs.beginText();
            cs.newLineAtOffset(x, y);
            cs.showText(text);
            cs.endText();
            y -= 14;
        }

        void writeLineRight(String text, float rightX) throws IOException {
            float tw = currentFont.getStringWidth(text) / 1000f * currentFontSize;
            cs.beginText();
            cs.newLineAtOffset(rightX - tw, y);
            cs.showText(text);
            cs.endText();
            y -= 16;
        }

        // Seller/buyer section: calculates box height and draws border around both columns
        void startSellerBuyerSection() {
            // We track where we started for the border
            sellerBuyerStartY = y + 14; // top of the last written line before the section
            sellerBuyerMaxX = MARGIN + TABLE_W + COL_WIDTHS[COL_WIDTHS.length - 1];
        }

        void endSellerBuyerSection() throws IOException {
            // Both seller and buyer data is already written; just calculate bottom
            // No rectangle border — cleaner look
        }

        // --- Table drawing ---

        void drawTableHeader() throws IOException {
            checkFooter(40);
            cs.setFont(fontBold, 8);
            // Header background
            cs.setNonStrokingColor(0.92f, 0.92f, 0.92f);
            cs.addRect(MARGIN, y - 3, TABLE_W, 12);
            cs.fill();
            cs.setNonStrokingColor(0, 0, 0);

            String[] headers = {"Lp.", "Produkt", "Ilo\u015B\u0107", "Cena netto", "Warto\u015B\u0107 netto", "VAT %", "Warto\u015B\u0107 brutto"};
            for (int i = 0; i < headers.length; i++) {
                cs.beginText();
                cs.newLineAtOffset(COL_STARTS[i] + 2, y);
                cs.showText(headers[i]);
                cs.endText();
                // Vertical border lines
                if (i > 0) {
                    cs.setStrokingColor(0.7f);
                    cs.moveTo(COL_STARTS[i], y - 2);
                    cs.lineTo(COL_STARTS[i], y + 8);
                    cs.stroke();
                }
            }
            // Top horizontal border
            cs.setStrokingColor(0.7f);
            cs.moveTo(MARGIN, y + 8);
            cs.lineTo(MARGIN + TABLE_W, y + 8);
            cs.stroke();
            // Bottom of header
            cs.moveTo(MARGIN, y - 2);
            cs.lineTo(MARGIN + TABLE_W, y - 2);
            cs.stroke();
            cs.setStrokingColor(0, 0, 0);
            y -= 2 + 12;
        }

        void drawTableRow(String[] values) throws IOException {
            float rowH = 12;
            cs.setFont(fontReg, 8);

            // Vertical borders
            for (int i = 0; i < COL_STARTS.length; i++) {
                cs.setStrokingColor(0.7f);
                cs.moveTo(COL_STARTS[i], y);
                cs.lineTo(COL_STARTS[i], y - rowH);
                cs.stroke();
            }
            // Rightmost border
            float rightX = COL_STARTS[COL_STARTS.length - 1] + COL_WIDTHS[COL_WIDTHS.length - 1];
            cs.moveTo(rightX, y);
            cs.lineTo(rightX, y - rowH);
            cs.stroke();

            // Text
            float textY = y - 8;
            for (int i = 0; i < values.length; i++) {
                cs.beginText();
                cs.newLineAtOffset(COL_STARTS[i] + 2, textY);
                cs.showText(values[i]);
                cs.endText();
            }
            y -= rowH;
        }

        void drawTableBottomLine() throws IOException {
            cs.setStrokingColor(0.7f);
            float rightX = COL_STARTS[COL_STARTS.length - 1] + COL_WIDTHS[COL_WIDTHS.length - 1];
            cs.moveTo(MARGIN, y);
            cs.lineTo(rightX, y);
            cs.stroke();
            cs.setStrokingColor(0, 0, 0);
        }

        // --- Totals with gray background ---

        void drawTotals(BigDecimal totalNet, BigDecimal totalVat, BigDecimal totalGross) throws IOException {
            checkFooter(50);
            float boxH = 36;
            float boxX = MARGIN + TABLE_W - 180;
            float boxW = 180;

            // Gray background
            cs.setNonStrokingColor(0.95f, 0.95f, 0.95f);
            cs.addRect(boxX, y - boxH + 4, boxW, boxH);
            cs.fill();
            cs.setNonStrokingColor(0, 0, 0);

            // Border
            cs.setStrokingColor(0.5f);
            cs.addRect(boxX, y - boxH + 4, boxW, boxH);
            cs.stroke();
            cs.setStrokingColor(0, 0, 0);

            float ty = y - 4;
            cs.setFont(fontBold, 9);
            writeLineRight("Razem netto: " + formatPrice(totalNet), boxX + boxW - 4);
            writeLineRight("W tym VAT: " + formatPrice(totalVat), boxX + boxW - 4);
            cs.setFont(fontBold, 11);
            writeLineRight("RAZEM BRUTTO: " + formatPrice(totalGross), boxX + boxW - 4);
            y = ty - boxH + 4;
        }

        // --- Status stamp ---

        void drawStatusStamp() throws IOException {
            checkFooter(40);
            if (invoice.getStatus() == InvoiceStatus.DRAFT) {
                y -= 16;
                cs.setFont(fontBold, 14);
                cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("SZKIC");
                cs.endText();
                cs.setNonStrokingColor(0, 0, 0);
            } else if (invoice.getStatus() == InvoiceStatus.PAID) {
                y -= 16;
                cs.setFont(fontBold, 16);
                cs.setNonStrokingColor(0, 0.6f, 0);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("ZAP\u0141ACONO");
                cs.endText();
                cs.setNonStrokingColor(0, 0, 0);
            } else if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                y -= 16;
                cs.setFont(fontBold, 16);
                cs.setNonStrokingColor(0.8f, 0, 0);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("ANULOWANO");
                cs.endText();
                cs.setNonStrokingColor(0, 0, 0);
            }
        }
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

    private String generateDraftNumber() {
        String prefix = "SZKIC/" + Year.now().getValue() + "/";
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

    private static PDFont loadFont(PDDocument doc, String resourcePath) {
        try (InputStream is = InvoiceService.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            return PDType0Font.load(doc, is);
        } catch (IOException e) {
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
    }

    private static String formatPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP).toString() + " PLN";
    }

    private static String formatVat(BigDecimal vatRate) {
        return vatRate.setScale(0, RoundingMode.HALF_UP).toString() + "%";
    }
}
