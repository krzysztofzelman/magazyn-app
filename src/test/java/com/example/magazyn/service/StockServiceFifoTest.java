package com.example.magazyn.service;

import com.example.magazyn.dto.StockMovementRequest;
import com.example.magazyn.dto.StockMovementResponse;
import com.example.magazyn.entity.Batch;
import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.StockMovement;
import com.example.magazyn.repository.BatchRepository;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceFifoTest {

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

    private Batch createBatch(Long id, Product product, String lotNumber, int quantity,
                              LocalDate createdAtDate) {
        return Batch.builder()
                .id(id)
                .product(product)
                .lotNumber(lotNumber)
                .expiryDate(createdAtDate.plusMonths(6))
                .quantity(quantity)
                .createdAt(createdAtDate.atStartOfDay())
                .build();
    }

    private StockMovementRequest createRequest(MovementType type, int quantity, Long batchId) {
        StockMovementRequest request = new StockMovementRequest();
        request.setType(type);
        request.setQuantity(quantity);
        request.setBatchId(batchId);
        return request;
    }

    // ──────────────────────────────────────────────
    // FIFO: WYDANIE from a single batch (no batchId → auto FIFO)
    // ──────────────────────────────────────────────

    @Test
    void wydanieFIFO_singleBatch_deductsCorrectly() {
        Product product = createProduct(1L, "Produkt A", "A-001", 100);
        Batch batch = createBatch(10L, product, "LOT-001", 50, LocalDate.of(2025, 1, 1));

        StockMovementRequest request = createRequest(MovementType.WYDANIE, 20, null);

        when(batchRepository.findByProductIdOrderByCreatedAtAscForUpdate(1L, anyLong())).thenReturn(List.of(batch));
        when(productRepository.findByIdForUpdate(1L, anyLong())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            return StockMovement.builder()
                    .id(200L)
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
        assertEquals(20, response.getQuantity());
        assertEquals(80, product.getQuantity()); // 100 - 20
        assertEquals(30, batch.getQuantity());   // 50 - 20

        verify(batchRepository).findByProductIdOrderByCreatedAtAscForUpdate(1L, anyLong());
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    // ──────────────────────────────────────────────
    // FIFO: WYDANIE from multiple batches
    // ──────────────────────────────────────────────

    @Test
    void wydanieFIFO_multipleBatches_deductsFromOldestFirst() {
        Product product = createProduct(1L, "Produkt A", "A-001", 100);
        Batch batch1 = createBatch(10L, product, "LOT-001", 10, LocalDate.of(2025, 1, 1));
        Batch batch2 = createBatch(11L, product, "LOT-002", 20, LocalDate.of(2025, 2, 1));
        Batch batch3 = createBatch(12L, product, "LOT-003", 30, LocalDate.of(2025, 3, 1));

        StockMovementRequest request = createRequest(MovementType.WYDANIE, 40, null);

        when(productRepository.findByIdForUpdate(1L, anyLong())).thenReturn(Optional.of(product));
        when(batchRepository.findByProductIdOrderByCreatedAtAscForUpdate(1L, anyLong()))
                .thenReturn(List.of(batch1, batch2, batch3));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            return StockMovement.builder()
                    .id(200L)
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
        assertEquals(40, response.getQuantity());
        assertEquals(60, product.getQuantity()); // 100 - 40

        // batch1 (oldest): fully consumed (10)
        assertEquals(0, batch1.getQuantity());
        // batch2: fully consumed (20)
        assertEquals(0, batch2.getQuantity());
        // batch3: partially consumed (30 - 10 = 20 left)
        assertEquals(20, batch3.getQuantity());

        verify(batchRepository).findByProductIdOrderByCreatedAtAscForUpdate(1L, anyLong());
        verify(batchRepository, times(3)).save(any(Batch.class));
    }

    // ──────────────────────────────────────────────
    // WYDANIE with specific batchId
    // ──────────────────────────────────────────────

    @Test
    void wydanie_withSpecificBatchId_deductsFromThatBatch() {
        Product product = createProduct(1L, "Produkt A", "A-001", 100);
        Batch batch1 = createBatch(10L, product, "LOT-001", 30, LocalDate.of(2025, 1, 1));
        Batch batch2 = createBatch(11L, product, "LOT-002", 30, LocalDate.of(2025, 2, 1));

        StockMovementRequest request = createRequest(MovementType.WYDANIE, 10, 11L); // batch2

        when(productRepository.findByIdForUpdate(1L, anyLong())).thenReturn(Optional.of(product));
        when(batchRepository.findByIdForUpdate(11L, anyLong())).thenReturn(Optional.of(batch2));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            return StockMovement.builder()
                    .id(200L)
                    .product(m.getProduct())
                    .type(m.getType())
                    .quantity(m.getQuantity())
                    .note(m.getNote())
                    .createdBy(m.getCreatedBy())
                    .batchId(m.getBatchId())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        StockMovementResponse response = stockService.addMovement(1L, request, USERNAME);

        assertNotNull(response);
        assertEquals(10, response.getQuantity());
        assertEquals(90, product.getQuantity()); // 100 - 10
        assertEquals(30, batch1.getQuantity());  // unchanged
        assertEquals(20, batch2.getQuantity());  // 30 - 10
        assertEquals(Long.valueOf(11L), response.getBatchId());

        verify(batchRepository).findByIdForUpdate(11L, anyLong());
        verify(batchRepository, never()).findByProductIdOrderByCreatedAtAscForUpdate(any(), anyLong());
    }

    // ──────────────────────────────────────────────
    // Insufficient stock (FIFO)
    // ──────────────────────────────────────────────

    @Test
    void wydanieFIFO_insufficientBatchStock_throws() {
        Product product = createProduct(1L, "Produkt A", "A-001", 5);
        Batch batch = createBatch(10L, product, "LOT-001", 5, LocalDate.of(2025, 1, 1));

        StockMovementRequest request = createRequest(MovementType.WYDANIE, 10, null);

        when(productRepository.findByIdForUpdate(1L, anyLong())).thenReturn(Optional.of(product));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> stockService.addMovement(1L, request, USERNAME));

        assertTrue(ex.getMessage().contains("Insufficient"));
        assertTrue(ex.getMessage().contains("5"));
        verify(productRepository, never()).save(any());
        verify(batchRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // PRZYJECIE with batchId (adding to existing batch)
    // ──────────────────────────────────────────────

    @Test
    void przyjecie_withBatchId_addsToExistingBatch() {
        Product product = createProduct(1L, "Produkt A", "A-001", 50);
        Batch batch = createBatch(10L, product, "LOT-001", 30, LocalDate.of(2025, 1, 1));

        StockMovementRequest request = createRequest(MovementType.PRZYJECIE, 20, 10L);

        when(productRepository.findByIdForUpdate(1L, anyLong())).thenReturn(Optional.of(product));
        when(batchRepository.findByIdForUpdate(10L, anyLong())).thenReturn(Optional.of(batch));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(batchRepository.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            return StockMovement.builder()
                    .id(200L)
                    .product(m.getProduct())
                    .type(m.getType())
                    .quantity(m.getQuantity())
                    .note(m.getNote())
                    .createdBy(m.getCreatedBy())
                    .batchId(m.getBatchId())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        StockMovementResponse response = stockService.addMovement(1L, request, USERNAME);

        assertNotNull(response);
        assertEquals(20, response.getQuantity());
        assertEquals(70, product.getQuantity()); // 50 + 20
        assertEquals(50, batch.getQuantity());   // 30 + 20
        assertEquals(Long.valueOf(10L), response.getBatchId());

        verify(batchRepository).findByIdForUpdate(10L, anyLong());
    }
}
