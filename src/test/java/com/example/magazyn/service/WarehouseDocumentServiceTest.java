package com.example.magazyn.service;

import com.example.magazyn.dto.WarehouseDocumentItemRequest;
import com.example.magazyn.dto.WarehouseDocumentRequest;
import com.example.magazyn.dto.WarehouseDocumentResponse;
import com.example.magazyn.entity.*;
import com.example.magazyn.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseDocumentServiceTest {

    @Mock
    private WarehouseDocumentRepository documentRepository;

    @Mock
    private WarehouseDocumentItemRepository itemRepository;

    @Mock
    private ContractorRepository contractorRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WarehouseDocumentService documentService;

    private static final String USERNAME = "testuser";

    private Contractor contractor;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        contractor = Contractor.builder()
                .id(1L)
                .name("Test Contractor")
                .taxId("1234567890")
                .type(ContractorType.SUPPLIER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        productA = Product.builder()
                .id(1L)
                .name("Product A")
                .sku("SKU-A")
                .unit("szt.")
                .quantity(100)
                .price(BigDecimal.valueOf(50))
                .build();

        productB = Product.builder()
                .id(2L)
                .name("Product B")
                .sku("SKU-B")
                .unit("szt.")
                .quantity(50)
                .price(BigDecimal.valueOf(30))
                .build();
    }

    private WarehouseDocumentItem createItem(Long id, WarehouseDocument doc, Product product, int qty) {
        return WarehouseDocumentItem.builder()
                .id(id)
                .document(doc)
                .product(product)
                .quantity(qty)
                .unitPrice(product.getPrice())
                .build();
    }

    private WarehouseDocument createDocument(Long id, DocumentType type, DocumentStatus status,
                                             List<WarehouseDocumentItem> items) {
        WarehouseDocument doc = WarehouseDocument.builder()
                .id(id)
                .number(type.name() + "/2026/" + String.format("%03d", id))
                .type(type)
                .contractor(contractor)
                .status(status)
                .createdBy(USERNAME)
                .createdAt(LocalDateTime.now())
                .items(items)
                .build();
        if (items != null) {
            items.forEach(item -> item.setDocument(doc));
        }
        return doc;
    }

    private WarehouseDocumentRequest createRequest(DocumentType type, Long contractorId,
                                                   List<WarehouseDocumentItemRequest> items) {
        WarehouseDocumentRequest request = new WarehouseDocumentRequest();
        request.setType(type);
        request.setContractorId(contractorId);
        request.setItems(items);
        request.setNotes("Test notes");
        return request;
    }

    private WarehouseDocumentItemRequest createItemRequest(Long productId, int qty) {
        WarehouseDocumentItemRequest req = new WarehouseDocumentItemRequest();
        req.setProductId(productId);
        req.setQuantity(qty);
        req.setUnitPrice(BigDecimal.valueOf(10));
        return req;
    }

    // ──────────────────────────────────────────────
    // createDocument
    // ──────────────────────────────────────────────

    @Test
    void createDocument_PZ_createsDocumentWithItems() {
        when(contractorRepository.findById(1L)).thenReturn(Optional.of(contractor));
        when(productRepository.findById(1L)).thenReturn(Optional.of(productA));
        when(productRepository.findById(2L)).thenReturn(Optional.of(productB));
        when(documentRepository.findMaxNumberByTypeAndYear(eq(DocumentType.PZ), startsWith("PZ/2026/")))
                .thenReturn(Optional.empty());
        when(documentRepository.save(any(WarehouseDocument.class)))
                .thenAnswer(invocation -> {
                    WarehouseDocument doc = invocation.getArgument(0);
                    doc.setId(1L);
                    return doc;
                });

        WarehouseDocumentRequest request = createRequest(DocumentType.PZ, 1L,
                Arrays.asList(createItemRequest(1L, 10), createItemRequest(2L, 5)));

        WarehouseDocumentResponse response = documentService.createDocument(request, USERNAME);

        assertNotNull(response);
        assertEquals(DocumentType.PZ, response.getType());
        assertEquals(DocumentStatus.DRAFT, response.getStatus());
        assertEquals("PZ/2026/001", response.getNumber());
        assertEquals(2, response.getItems().size());
        verify(documentRepository).save(any(WarehouseDocument.class));
        verify(auditLogService).log(eq(USERNAME), eq("DOCUMENT_CREATE"), eq("WarehouseDocument"), eq(1L), anyString());
    }

    @Test
    void createDocument_WZ_generatesSequentialNumber() {
        when(contractorRepository.findById(1L)).thenReturn(Optional.of(contractor));
        when(productRepository.findById(1L)).thenReturn(Optional.of(productA));
        when(documentRepository.findMaxNumberByTypeAndYear(eq(DocumentType.WZ), startsWith("WZ/2026/")))
                .thenReturn(Optional.of("WZ/2026/005"));
        when(documentRepository.save(any(WarehouseDocument.class)))
                .thenAnswer(invocation -> {
                    WarehouseDocument doc = invocation.getArgument(0);
                    doc.setId(2L);
                    return doc;
                });

        WarehouseDocumentRequest request = createRequest(DocumentType.WZ, 1L,
                List.of(createItemRequest(1L, 10)));

        WarehouseDocumentResponse response = documentService.createDocument(request, USERNAME);

        assertEquals("WZ/2026/006", response.getNumber());
    }

    // ──────────────────────────────────────────────
    // confirmDocument — PZ
    // ──────────────────────────────────────────────

    @Test
    void confirmDocument_PZ_createsStockMovementsAndIncreasesStock() {
        List<WarehouseDocumentItem> items = Arrays.asList(
                createItem(10L, null, productA, 5),
                createItem(11L, null, productB, 3)
        );
        WarehouseDocument doc = createDocument(1L, DocumentType.PZ, DocumentStatus.DRAFT, items);

        when(documentRepository.findByIdWithItemsLocked(1L)).thenReturn(Optional.of(doc));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(productA));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(productB));
        when(documentRepository.save(any(WarehouseDocument.class))).thenReturn(doc);

        WarehouseDocumentResponse response = documentService.confirmDocument(1L, USERNAME);

        assertEquals(DocumentStatus.CONFIRMED, response.getStatus());
        assertNotNull(response.getConfirmedAt());
        verify(productRepository, times(2)).save(any(Product.class));
        verify(stockMovementRepository, times(2)).save(any(StockMovement.class));
        assertEquals(105, productA.getQuantity()); // 100 + 5
        assertEquals(53, productB.getQuantity());  // 50 + 3
        verify(auditLogService).log(eq(USERNAME), eq("DOCUMENT_CONFIRM"), eq("WarehouseDocument"), eq(1L), anyString());
    }

    // ──────────────────────────────────────────────
    // confirmDocument — WZ happy path
    // ──────────────────────────────────────────────

    @Test
    void confirmDocument_WZ_createsStockMovementsAndDecreasesStock() {
        List<WarehouseDocumentItem> items = Arrays.asList(
                createItem(10L, null, productA, 5),
                createItem(11L, null, productB, 3)
        );
        WarehouseDocument doc = createDocument(1L, DocumentType.WZ, DocumentStatus.DRAFT, items);

        when(documentRepository.findByIdWithItemsLocked(1L)).thenReturn(Optional.of(doc));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(productA));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(productB));
        when(documentRepository.save(any(WarehouseDocument.class))).thenReturn(doc);

        WarehouseDocumentResponse response = documentService.confirmDocument(1L, USERNAME);

        assertEquals(DocumentStatus.CONFIRMED, response.getStatus());
        assertNotNull(response.getConfirmedAt());
        assertEquals(95, productA.getQuantity());  // 100 - 5
        assertEquals(47, productB.getQuantity());  // 50 - 3
        verify(stockMovementRepository, times(2)).save(any(StockMovement.class));
    }

    // ──────────────────────────────────────────────
    // confirmDocument — WZ insufficient stock
    // ──────────────────────────────────────────────

    @Test
    void confirmDocument_WZ_insufficientStock_throwsException() {
        // ProductA has 100, productB has 50 — request more than available
        List<WarehouseDocumentItem> items = Arrays.asList(
                createItem(10L, null, productA, 200),
                createItem(11L, null, productB, 3)
        );
        WarehouseDocument doc = createDocument(1L, DocumentType.WZ, DocumentStatus.DRAFT, items);

        when(documentRepository.findByIdWithItemsLocked(1L)).thenReturn(Optional.of(doc));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(productA));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(productB));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.confirmDocument(1L, USERNAME));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertTrue(ex.getMessage().contains("Product A"));
        verify(stockMovementRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // confirmDocument — non-DRAFT status
    // ──────────────────────────────────────────────

    @Test
    void confirmDocument_alreadyConfirmed_throwsException() {
        WarehouseDocument doc = createDocument(1L, DocumentType.PZ, DocumentStatus.CONFIRMED, List.of());

        when(documentRepository.findByIdWithItemsLocked(1L)).thenReturn(Optional.of(doc));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.confirmDocument(1L, USERNAME));

        assertTrue(ex.getMessage().contains("Only DRAFT documents can be confirmed"));
    }

    // ──────────────────────────────────────────────
    // cancelDocument
    // ──────────────────────────────────────────────

    @Test
    void cancelDocument_DRAFT_setsCancelledStatus() {
        WarehouseDocument doc = createDocument(1L, DocumentType.PZ, DocumentStatus.DRAFT, List.of());
        when(documentRepository.findByIdWithItems(1L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(WarehouseDocument.class))).thenReturn(doc);

        WarehouseDocumentResponse response = documentService.cancelDocument(1L, USERNAME);

        assertEquals(DocumentStatus.CANCELLED, response.getStatus());
        verify(documentRepository).save(any(WarehouseDocument.class));
        verify(auditLogService).log(eq(USERNAME), eq("DOCUMENT_CANCEL"), eq("WarehouseDocument"), eq(1L), anyString());
    }

    @Test
    void cancelDocument_alreadyConfirmed_throwsException() {
        WarehouseDocument doc = createDocument(1L, DocumentType.PZ, DocumentStatus.CONFIRMED, List.of());
        when(documentRepository.findByIdWithItems(1L)).thenReturn(Optional.of(doc));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.cancelDocument(1L, USERNAME));

        assertTrue(ex.getMessage().contains("Only DRAFT documents can be cancelled"));
        verify(documentRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // getDocumentById
    // ──────────────────────────────────────────────

    @Test
    void getDocumentById_returnsDocumentWithItems() {
        List<WarehouseDocumentItem> items = List.of(createItem(10L, null, productA, 5));
        WarehouseDocument doc = createDocument(1L, DocumentType.PZ, DocumentStatus.DRAFT, items);

        when(documentRepository.findByIdWithItems(1L)).thenReturn(Optional.of(doc));

        WarehouseDocumentResponse response = documentService.getDocumentById(1L);

        assertNotNull(response);
        assertEquals("PZ/2026/001", response.getNumber());
        assertEquals(1, response.getItems().size());
        assertEquals("Product A", response.getItems().get(0).getProductName());
        assertEquals(Integer.valueOf(5), response.getItems().get(0).getQuantity());
    }

    @Test
    void getDocumentById_notFound_throwsException() {
        when(documentRepository.findByIdWithItems(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> documentService.getDocumentById(99L));
    }
}
