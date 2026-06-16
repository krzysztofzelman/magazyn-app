package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.StockMovementRequest;
import com.example.magazyn.dto.StockMovementResponse;
import com.example.magazyn.dto.StockResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.StockMovement;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.BatchRepository;
import com.example.magazyn.repository.StockMovementRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private StockService stockService;

    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Product createProduct(Long id, String name, String sku, int quantity) {
        return Product.builder()
                .id(id)
                .name(name)
                .sku(sku)
                .unit("szt.")
                .quantity(quantity)
                .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();
    }

    private StockMovementRequest createRequest(MovementType type, int quantity, String note) {
        StockMovementRequest request = new StockMovementRequest();
        request.setType(type);
        request.setQuantity(quantity);
        request.setNote(note);
        return request;
    }

    // ──────────────────────────────────────────────
    // addMovement — happy paths
    // ──────────────────────────────────────────────

    @Test
    void addMovement_przyjecie_increasesQuantity() {
        Product product = createProduct(1L, "Produkt A", "A-001", 10);
        StockMovementRequest request = createRequest(MovementType.PRZYJECIE, 5, "Dostawa");

        when(productRepository.findByIdForUpdate(eq(1L), anyLong())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            return StockMovement.builder()
                    .id(100L)
                    .product(m.getProduct())
                    .type(m.getType())
                    .quantity(m.getQuantity())
                    .note(m.getNote())
                    .createdBy(m.getCreatedBy())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        StockMovementResponse response = stockService.addMovement(1L, request, USERNAME);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(MovementType.PRZYJECIE, response.getType());
        assertEquals(5, response.getQuantity());
        assertEquals(USERNAME, response.getCreatedBy());
        assertEquals(15, product.getQuantity()); // 10 + 5

        verify(productRepository).findByIdForUpdate(eq(1L), anyLong());
        verify(productRepository).save(product);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void addMovement_wydanie_decreasesQuantity() {
        Product product = createProduct(1L, "Produkt A", "A-001", 20);
        StockMovementRequest request = createRequest(MovementType.WYDANIE, 8, "Wydanie do klienta");

        when(productRepository.findByIdForUpdate(eq(1L), anyLong())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            return StockMovement.builder()
                    .id(101L)
                    .product(m.getProduct())
                    .type(m.getType())
                    .quantity(m.getQuantity())
                    .note(m.getNote())
                    .createdBy(m.getCreatedBy())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        StockMovementResponse response = stockService.addMovement(1L, request, USERNAME);

        assertNotNull(response);
        assertEquals(MovementType.WYDANIE, response.getType());
        assertEquals(8, response.getQuantity());
        assertEquals(12, product.getQuantity()); // 20 - 8

        verify(productRepository).findByIdForUpdate(eq(1L), anyLong());
        verify(productRepository).save(product);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void addMovement_korekta_setsAbsoluteQuantity() {
        Product product = createProduct(1L, "Produkt A", "A-001", 100);
        StockMovementRequest request = createRequest(MovementType.KOREKTA, 50, "Korekta stanu");

        when(productRepository.findByIdForUpdate(eq(1L), anyLong())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            return StockMovement.builder()
                    .id(102L)
                    .product(m.getProduct())
                    .type(m.getType())
                    .quantity(m.getQuantity())
                    .note(m.getNote())
                    .createdBy(m.getCreatedBy())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        StockMovementResponse response = stockService.addMovement(1L, request, USERNAME);

        assertEquals(50, product.getQuantity()); // korekta sets absolute value
        assertEquals(50, response.getQuantity());

        verify(productRepository).findByIdForUpdate(eq(1L), anyLong());
        verify(productRepository).save(product);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    // ──────────────────────────────────────────────
    // addMovement — error cases
    // ──────────────────────────────────────────────

    @Test
    void addMovement_productNotFound_throws() {
        when(productRepository.findByIdForUpdate(eq(999L), anyLong())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> stockService.addMovement(999L, createRequest(MovementType.PRZYJECIE, 5, null), USERNAME));

        assertTrue(ex.getMessage().contains("not found"));
        verify(productRepository).findByIdForUpdate(eq(999L), anyLong());
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void addMovement_quantityNull_throws() {
        Product product = createProduct(1L, "Produkt A", "A-001", 10);
        StockMovementRequest request = new StockMovementRequest();
        request.setType(MovementType.PRZYJECIE);
        request.setQuantity(null);

        when(productRepository.findByIdForUpdate(eq(1L), anyLong())).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> stockService.addMovement(1L, request, USERNAME));

        assertTrue(ex.getMessage().contains("required"));
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void addMovement_quantityNonPositive_throws() {
        Product product = createProduct(1L, "Produkt A", "A-001", 10);
        StockMovementRequest request = createRequest(MovementType.PRZYJECIE, 0, null);

        when(productRepository.findByIdForUpdate(eq(1L), anyLong())).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> stockService.addMovement(1L, request, USERNAME));

        assertTrue(ex.getMessage().contains("positive"));
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void addMovement_typeNull_throws() {
        Product product = createProduct(1L, "Produkt A", "A-001", 10);
        StockMovementRequest request = new StockMovementRequest();
        request.setType(null);
        request.setQuantity(5);

        when(productRepository.findByIdForUpdate(eq(1L), anyLong())).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> stockService.addMovement(1L, request, USERNAME));

        assertTrue(ex.getMessage().contains("required"));
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void addMovement_insufficientStock_throws() {
        Product product = createProduct(1L, "Produkt A", "A-001", 3);
        StockMovementRequest request = createRequest(MovementType.WYDANIE, 10, null);

        when(productRepository.findByIdForUpdate(eq(1L), anyLong())).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> stockService.addMovement(1L, request, USERNAME));

        assertTrue(ex.getMessage().contains("Insufficient"));
        assertTrue(ex.getMessage().contains("3"));
        verify(productRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // getMovements
    // ──────────────────────────────────────────────

    @Test
    void getMovements_returnsList() {
        Product product = createProduct(1L, "Produkt A", "A-001", 10);
        StockMovement movement = StockMovement.builder()
                .id(1L)
                .product(product)
                .type(MovementType.PRZYJECIE)
                .quantity(10)
                .note("Dostawa")
                .createdBy(USERNAME)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.existsByIdAndTenantId(eq(1L), any())).thenReturn(true);
        when(stockMovementRepository.findByProductIdAndTenantIdOrderByCreatedAtDesc(eq(1L), anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movement)));

        List<StockMovementResponse> result = stockService.getMovements(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(MovementType.PRZYJECIE, result.get(0).getType());
        assertEquals(10, result.get(0).getQuantity());

        verify(productRepository).existsByIdAndTenantId(eq(1L), any());
        verify(stockMovementRepository).findByProductIdAndTenantIdOrderByCreatedAtDesc(eq(1L), anyLong(), any());
    }

    @Test
    void getMovements_productNotFound_throws() {
        when(productRepository.existsByIdAndTenantId(eq(999L), any())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> stockService.getMovements(999L));

        assertTrue(ex.getMessage().contains("not found"));
        verify(productRepository).existsByIdAndTenantId(eq(999L), any());
        verifyNoInteractions(stockMovementRepository);
    }

    // ──────────────────────────────────────────────
    // getStock
    // ──────────────────────────────────────────────

    @Test
    void getStock_returnsStockInfo() {
        Product product = createProduct(1L, "Produkt A", "A-001", 42);

        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(product));

        StockResponse response = stockService.getStock(1L);

        assertNotNull(response);
        assertEquals(1L, response.getProductId());
        assertEquals("Produkt A", response.getProductName());
        assertEquals("A-001", response.getSku());
        assertEquals(42, response.getQuantity());

        verify(productRepository).findByIdAndTenantId(eq(1L), any());
    }

    @Test
    void getStock_productNotFound_throws() {
        when(productRepository.findByIdAndTenantId(eq(999L), any())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> stockService.getStock(999L));

        assertTrue(ex.getMessage().contains("not found"));
        verify(productRepository).findByIdAndTenantId(eq(999L), any());
    }
}
