package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.CreateInvoiceItemRequest;
import com.example.magazyn.dto.CreateInvoiceRequest;
import com.example.magazyn.dto.InvoiceResponse;
import com.example.magazyn.entity.*;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private WarehouseDocumentRepository documentRepository;

    @Mock
    private CompanySettingsRepository companySettingsRepository;

    @Mock
    private ContractorRepository contractorRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InvoiceService invoiceService;

    @Captor
    private ArgumentCaptor<Invoice> invoiceCaptor;

    private static final Long TENANT_ID = 1L;
    private static final Long INVOICE_ID = 42L;
    private static final String USERNAME = "testuser";

    private CompanySettings defaultSeller;
    private CreateInvoiceRequest validRequest;
    private Invoice draftInvoice;
    private Invoice issuedInvoice;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        defaultSeller = CompanySettings.builder()
                .id(1L)
                .name("Moja Firma Sp. z o.o.")
                .taxId("1234567890")
                .address("ul. Główna 1, 00-001 Warszawa")
                .bankAccount("PL60102000000000000000000000")
                .build();

        var item1 = new CreateInvoiceItemRequest(
                "Produkt A", "SKU-001", "szt.", 2,
                new BigDecimal("100.00"), new BigDecimal("23.00"));
        var item2 = new CreateInvoiceItemRequest(
                "Produkt B", "SKU-002", "szt.", 3,
                new BigDecimal("50.00"), new BigDecimal("8.00"));

        validRequest = new CreateInvoiceRequest(
                "Kupiec Sp. z o.o.", "9876543210", "ul. Klienta 5, 00-002 Kraków",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 24),
                "PRZELEW", "PL61109000000000000000000001", null,
                List.of(item1, item2));

        // Expected totals for validRequest:
        // Item 1: 2 x 100.00 = 200.00 net, VAT 23% = 46.00, gross = 246.00
        // Item 2: 3 x 50.00 = 150.00 net, VAT 8% = 12.00, gross = 162.00
        // Total net: 350.00, total VAT: 58.00, total gross: 408.00

        draftInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .number("SZKIC/2026/001")
                .status(InvoiceStatus.DRAFT)
                .sellerName(defaultSeller.getName())
                .sellerTaxId(defaultSeller.getTaxId())
                .sellerAddress(defaultSeller.getAddress())
                .sellerBankAccount(defaultSeller.getBankAccount())
                .buyerName("Kupiec Sp. z o.o.")
                .buyerTaxId("9876543210")
                .buyerAddress("ul. Klienta 5, 00-002 Kraków")
                .issueDate(LocalDate.now())
                .saleDate(LocalDate.of(2026, 6, 10))
                .dueDate(LocalDate.of(2026, 6, 24))
                .paymentMethod("PRZELEW")
                .paymentAccount("PL61109000000000000000000001")
                .totalNet(new BigDecimal("350.00"))
                .totalVat(new BigDecimal("58.00"))
                .totalGross(new BigDecimal("408.00"))
                .createdBy(USERNAME)
                .createdAt(LocalDateTime.now())
                .items(new java.util.ArrayList<>())
                .version(0)
                .build();

        // Add items
        draftInvoice.getItems().add(InvoiceItem.builder()
                .id(1L)
                .invoice(draftInvoice)
                .productName("Produkt A")
                .productSku("SKU-001")
                .productUnit("szt.")
                .quantity(2)
                .unitPriceNet(new BigDecimal("100.00"))
                .vatRate(new BigDecimal("23.00"))
                .vatAmount(new BigDecimal("46.00"))
                .totalNet(new BigDecimal("200.00"))
                .totalGross(new BigDecimal("246.00"))
                .build());

        draftInvoice.getItems().add(InvoiceItem.builder()
                .id(2L)
                .invoice(draftInvoice)
                .productName("Produkt B")
                .productSku("SKU-002")
                .productUnit("szt.")
                .quantity(3)
                .unitPriceNet(new BigDecimal("50.00"))
                .vatRate(new BigDecimal("8.00"))
                .vatAmount(new BigDecimal("12.00"))
                .totalNet(new BigDecimal("150.00"))
                .totalGross(new BigDecimal("162.00"))
                .build());

        issuedInvoice = Invoice.builder()
                .id(INVOICE_ID + 1)
                .number("FV/2026/001")
                .status(InvoiceStatus.ISSUED)
                .sellerName(defaultSeller.getName())
                .sellerTaxId(defaultSeller.getTaxId())
                .sellerAddress(defaultSeller.getAddress())
                .sellerBankAccount(defaultSeller.getBankAccount())
                .buyerName("Kupiec Sp. z o.o.")
                .buyerTaxId("9876543210")
                .buyerAddress("ul. Klienta 5, 00-002 Kraków")
                .issueDate(LocalDate.now())
                .saleDate(LocalDate.of(2026, 6, 10))
                .dueDate(LocalDate.of(2026, 6, 24))
                .paymentMethod("PRZELEW")
                .paymentAccount("PL61109000000000000000000001")
                .totalNet(new BigDecimal("350.00"))
                .totalVat(new BigDecimal("58.00"))
                .totalGross(new BigDecimal("408.00"))
                .createdBy(USERNAME)
                .createdAt(LocalDateTime.now())
                .items(new java.util.ArrayList<>())
                .version(0)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ──────────────────────────────────────────────
    // createBlankInvoice
    // ──────────────────────────────────────────────

    @Test
    void createBlankInvoice_success() {
        when(companySettingsRepository.findByTenantId(TENANT_ID))
                .thenReturn(Optional.of(defaultSeller));
        when(invoiceRepository.findMaxNumberByPrefixAndTenantId("SZKIC/2026/", TENANT_ID))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice inv = i.getArgument(0);
                    inv.setId(INVOICE_ID);
                    inv.setCreatedAt(LocalDateTime.now());
                    return inv;
                });

        InvoiceResponse response = invoiceService.createBlankInvoice(validRequest, USERNAME);

        assertNotNull(response);
        assertEquals(INVOICE_ID, response.id());
        assertEquals("SZKIC/2026/001", response.number());
        assertEquals(InvoiceStatus.DRAFT, response.status());
        assertEquals("Kupiec Sp. z o.o.", response.buyerName());
        assertEquals(new BigDecimal("350.00"), response.totalNet());
        assertEquals(new BigDecimal("58.00"), response.totalVat());
        assertEquals(new BigDecimal("408.00"), response.totalGross());
        assertEquals(2, response.items().size());

        verify(companySettingsRepository).findByTenantId(TENANT_ID);
        verify(invoiceRepository).findMaxNumberByPrefixAndTenantId("SZKIC/2026/", TENANT_ID);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(auditLogService).log(eq(USERNAME), eq("INVOICE_DRAFT_CREATE"), eq("Invoice"), eq(INVOICE_ID), anyString());
    }

    @Test
    void createBlankInvoice_noCompanySettings_throws() {
        when(companySettingsRepository.findByTenantId(TENANT_ID))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.createBlankInvoice(validRequest, USERNAME));

        assertTrue(ex.getMessage().contains("Brak danych firmy"));
        verify(companySettingsRepository).findByTenantId(TENANT_ID);
        verify(invoiceRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // ──────────────────────────────────────────────
    // updateInvoice
    // ──────────────────────────────────────────────

    @Test
    void updateInvoice_success() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Updated request: change name and items
        var updatedItem = new CreateInvoiceItemRequest(
                "Produkt C", "SKU-003", "kg", 10,
                new BigDecimal("25.00"), new BigDecimal("5.00"));
        var updateRequest = new CreateInvoiceRequest(
                "Nowy Kupiec Sp. z o.o.", "1111111111", "ul. Nowa 10, 00-003 Wrocław",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15),
                "GOTOWKA", null, "Uwagi zaktualizowane",
                List.of(updatedItem));

        InvoiceResponse response = invoiceService.updateInvoice(INVOICE_ID, updateRequest, USERNAME);

        assertNotNull(response);
        // 10 x 25.00 = 250.00 net, VAT 5% = 12.50, gross = 262.50
        assertEquals(new BigDecimal("250.00"), response.totalNet());
        assertEquals(new BigDecimal("12.50"), response.totalVat());
        assertEquals(new BigDecimal("262.50"), response.totalGross());
        assertEquals("Nowy Kupiec Sp. z o.o.", response.buyerName());
        assertEquals("GOTOWKA", response.paymentMethod());
        assertEquals("Uwagi zaktualizowane", response.notes());

        verify(invoiceRepository).findByIdAndTenantId(INVOICE_ID, TENANT_ID);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(auditLogService).log(eq(USERNAME), eq("INVOICE_DRAFT_UPDATE"), eq("Invoice"), eq(INVOICE_ID), anyString());
    }

    @Test
    void updateInvoice_notFound_throws() {
        when(invoiceRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.updateInvoice(999L, validRequest, USERNAME));

        verify(invoiceRepository).findByIdAndTenantId(999L, TENANT_ID);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void updateInvoice_notDraft_throws() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(issuedInvoice));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> invoiceService.updateInvoice(INVOICE_ID, validRequest, USERNAME));

        assertTrue(ex.getMessage().contains("DRAFT"));
        verify(invoiceRepository).findByIdAndTenantId(INVOICE_ID, TENANT_ID);
        verify(invoiceRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // deleteInvoice
    // ──────────────────────────────────────────────

    @Test
    void deleteInvoice_success() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(draftInvoice));

        invoiceService.deleteInvoice(INVOICE_ID, USERNAME);

        verify(invoiceRepository).findByIdAndTenantId(INVOICE_ID, TENANT_ID);
        verify(invoiceRepository).delete(draftInvoice);
        verify(auditLogService).log(eq(USERNAME), eq("INVOICE_DRAFT_DELETE"), eq("Invoice"), eq(INVOICE_ID), anyString());
    }

    @Test
    void deleteInvoice_notFound_throws() {
        when(invoiceRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.deleteInvoice(999L, USERNAME));

        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void deleteInvoice_notDraft_throws() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(issuedInvoice));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> invoiceService.deleteInvoice(INVOICE_ID, USERNAME));

        assertTrue(ex.getMessage().contains("DRAFT"));
        verify(invoiceRepository, never()).delete(any());
    }

    // ──────────────────────────────────────────────
    // issueInvoice
    // ──────────────────────────────────────────────

    @Test
    void issueInvoice_success() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(draftInvoice));
        when(invoiceRepository.findMaxNumberByPrefixAndTenantId("FV/2026/", TENANT_ID))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(i -> i.getArgument(0));

        InvoiceResponse response = invoiceService.issueInvoice(INVOICE_ID, USERNAME);

        assertNotNull(response);
        assertEquals(InvoiceStatus.ISSUED, response.status());
        assertEquals("FV/2026/001", response.number());

        verify(invoiceRepository).findByIdAndTenantId(INVOICE_ID, TENANT_ID);
        verify(invoiceRepository).findMaxNumberByPrefixAndTenantId("FV/2026/", TENANT_ID);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(auditLogService).log(eq(USERNAME), eq("INVOICE_ISSUE"), eq("Invoice"), eq(INVOICE_ID), anyString());
    }

    @Test
    void issueInvoice_notDraft_throws() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(issuedInvoice));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> invoiceService.issueInvoice(INVOICE_ID, USERNAME));

        assertTrue(ex.getMessage().contains("DRAFT"));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void issueInvoice_notFound_throws() {
        when(invoiceRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.issueInvoice(999L, USERNAME));
    }

    // ──────────────────────────────────────────────
    // payInvoice
    // ──────────────────────────────────────────────

    @Test
    void payInvoice_success() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(issuedInvoice));
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(i -> i.getArgument(0));

        InvoiceResponse response = invoiceService.payInvoice(INVOICE_ID, "PRZELEW", "PL9999999999", USERNAME);

        assertNotNull(response);
        assertEquals(InvoiceStatus.PAID, response.status());
        assertNotNull(response.paidAt());
        assertEquals("PRZELEW", response.paymentMethod());
        assertEquals("PL9999999999", response.paymentAccount());

        verify(invoiceRepository).findByIdAndTenantId(INVOICE_ID, TENANT_ID);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(auditLogService).log(eq(USERNAME), eq("INVOICE_PAY"), eq("Invoice"), eq(INVOICE_ID), anyString());
    }

    @Test
    void payInvoice_notIssued_throws() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(draftInvoice));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> invoiceService.payInvoice(INVOICE_ID, null, null, USERNAME));

        assertTrue(ex.getMessage().contains("wystawione"));
        verify(invoiceRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // cancelInvoice
    // ──────────────────────────────────────────────

    @Test
    void cancelInvoice_success() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(issuedInvoice));
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(i -> i.getArgument(0));

        InvoiceResponse response = invoiceService.cancelInvoice(INVOICE_ID, USERNAME);

        assertNotNull(response);
        assertEquals(InvoiceStatus.CANCELLED, response.status());
        assertNotNull(response.cancelledAt());

        verify(invoiceRepository).findByIdAndTenantId(INVOICE_ID, TENANT_ID);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(auditLogService).log(eq(USERNAME), eq("INVOICE_CANCEL"), eq("Invoice"), eq(INVOICE_ID), anyString());
    }

    @Test
    void cancelInvoice_alreadyCancelled_throws() {
        Invoice cancelledInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .status(InvoiceStatus.CANCELLED)
                .build();

        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(cancelledInvoice));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> invoiceService.cancelInvoice(INVOICE_ID, USERNAME));

        assertTrue(ex.getMessage().contains("anulowana"));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_alreadyPaid_throws() {
        Invoice paidInvoice = Invoice.builder()
                .id(INVOICE_ID)
                .status(InvoiceStatus.PAID)
                .build();

        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(paidInvoice));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> invoiceService.cancelInvoice(INVOICE_ID, USERNAME));

        assertTrue(ex.getMessage().contains("opłaconej"));
        verify(invoiceRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // getAllInvoices
    // ──────────────────────────────────────────────

    @Test
    void getAllInvoices_noFilter() {
        when(invoiceRepository.findByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(List.of(issuedInvoice, draftInvoice));

        List<InvoiceResponse> result = invoiceService.getAllInvoices(null);

        assertEquals(2, result.size());
        verify(invoiceRepository).findByTenantIdOrderByCreatedAtDesc(TENANT_ID);
    }

    @Test
    void getAllInvoices_filterByStatus() {
        when(invoiceRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_ID, InvoiceStatus.DRAFT))
                .thenReturn(List.of(draftInvoice));

        List<InvoiceResponse> result = invoiceService.getAllInvoices("DRAFT");

        assertEquals(1, result.size());
        assertEquals(InvoiceStatus.DRAFT, result.get(0).status());
        verify(invoiceRepository).findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_ID, InvoiceStatus.DRAFT);
    }

    @Test
    void getAllInvoices_filterByYear() {
        when(invoiceRepository.findByTenantIdAndYear(TENANT_ID, 2026))
                .thenReturn(List.of(issuedInvoice));

        List<InvoiceResponse> result = invoiceService.getAllInvoices(null, 2026);

        assertEquals(1, result.size());
        verify(invoiceRepository).findByTenantIdAndYear(TENANT_ID, 2026);
    }

    @Test
    void getAllInvoices_filterByStatusAndYear() {
        when(invoiceRepository.findByTenantIdAndStatusAndYear(TENANT_ID, InvoiceStatus.ISSUED, 2026))
                .thenReturn(List.of(issuedInvoice));

        List<InvoiceResponse> result = invoiceService.getAllInvoices("ISSUED", 2026);

        assertEquals(1, result.size());
        verify(invoiceRepository).findByTenantIdAndStatusAndYear(TENANT_ID, InvoiceStatus.ISSUED, 2026);
    }

    // ──────────────────────────────────────────────
    // getAllInvoicesPaged
    // ──────────────────────────────────────────────

    @Test
    void getAllInvoicesPaged_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Invoice> invoicePage = new PageImpl<>(List.of(issuedInvoice, draftInvoice), pageable, 2);

        when(invoiceRepository.findByTenantId(TENANT_ID, pageable))
                .thenReturn(invoicePage);

        Page<InvoiceResponse> result = invoiceService.getAllInvoicesPaged(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(invoiceRepository).findByTenantId(TENANT_ID, pageable);
    }

    // ──────────────────────────────────────────────
    // getInvoice
    // ──────────────────────────────────────────────

    @Test
    void getInvoice_success() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID, TENANT_ID))
                .thenReturn(Optional.of(draftInvoice));

        InvoiceResponse response = invoiceService.getInvoice(INVOICE_ID);

        assertNotNull(response);
        assertEquals(INVOICE_ID, response.id());
        assertEquals(InvoiceStatus.DRAFT, response.status());
    }

    @Test
    void getInvoice_notFound_throws() {
        when(invoiceRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.getInvoice(999L));
    }

    // ──────────────────────────────────────────────
    // exportInvoicePdf
    // ──────────────────────────────────────────────

    @Test
    void exportInvoicePdf_success() {
        when(invoiceRepository.findByIdAndTenantId(INVOICE_ID + 1, TENANT_ID))
                .thenReturn(Optional.of(issuedInvoice));

        byte[] pdf = invoiceService.exportInvoicePdf(INVOICE_ID + 1);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        // Verify PDF starts with PDF magic bytes
        assertEquals('%', pdf[0]);
        assertEquals('P', pdf[1]);
        assertEquals('D', pdf[2]);
        assertEquals('F', pdf[3]);
    }

    @Test
    void exportInvoicePdf_notFound_throws() {
        when(invoiceRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.exportInvoicePdf(999L));
    }

    // ──────────────────────────────────────────────
    // generateFromDocument (basic smoke test)
    // ──────────────────────────────────────────────

    @Test
    void generateFromDocument_success() {
        Long docId = 100L;

        Contractor contractor = Contractor.builder()
                .id(1L)
                .name("Kupiec Sp. z o.o.")
                .taxId("9876543210")
                .address("ul. Klienta 5, 00-002 Kraków")
                .paymentMethod("PRZELEW")
                .paymentDays(14)
                .bankAccount("PL61109000000000000000000001")
                .build();

        Product product = Product.builder()
                .id(1L)
                .name("Produkt A")
                .sku("SKU-001")
                .unit("szt.")
                .defaultVatRate(new BigDecimal("23.00"))
                .build();

        WarehouseDocumentItem docItem = WarehouseDocumentItem.builder()
                .id(1L)
                .product(product)
                .quantity(5)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        WarehouseDocument doc = WarehouseDocument.builder()
                .id(docId)
                .type(DocumentType.WZ)
                .status(DocumentStatus.CONFIRMED)
                .number("WZ/2026/001")
                .contractor(contractor)
                .warehouseId(1L)
                .confirmedAt(LocalDateTime.now())
                .items(List.of(docItem))
                .build();

        when(documentRepository.findByIdWithItems(docId, anyLong()))
                .thenReturn(Optional.of(doc));
        when(invoiceRepository.findByDocumentIdAndTenantId(docId, TENANT_ID))
                .thenReturn(Optional.empty());
        when(companySettingsRepository.findByTenantId(TENANT_ID))
                .thenReturn(Optional.of(defaultSeller));
        when(invoiceRepository.findMaxNumberByPrefixAndTenantId("FV/2026/", TENANT_ID))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(i -> {
                    Invoice inv = i.getArgument(0);
                    inv.setId(INVOICE_ID);
                    inv.setCreatedAt(LocalDateTime.now());
                    return inv;
                });

        InvoiceResponse response = invoiceService.generateFromDocument(docId, USERNAME);

        assertNotNull(response);
        assertEquals(INVOICE_ID, response.id());
        assertEquals(InvoiceStatus.ISSUED, response.status());
        assertEquals("FV/2026/001", response.number());
        assertEquals(docId, response.documentId());
        assertEquals(1, response.items().size());
        // 5 x 100.00 = 500.00 net, VAT 23% = 115.00, gross = 615.00
        assertEquals(new BigDecimal("500.00"), response.totalNet());
        assertEquals(new BigDecimal("115.00"), response.totalVat());
        assertEquals(new BigDecimal("615.00"), response.totalGross());

        verify(documentRepository).findByIdWithItems(docId, anyLong());
        verify(companySettingsRepository).findByTenantId(TENANT_ID);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(auditLogService).log(eq(USERNAME), eq("INVOICE_CREATE"), eq("Invoice"), eq(INVOICE_ID), anyString());
    }
}
