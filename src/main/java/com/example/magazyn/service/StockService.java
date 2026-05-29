package com.example.magazyn.service;

import com.example.magazyn.dto.StockMovementRequest;
import com.example.magazyn.dto.StockMovementResponse;
import com.example.magazyn.dto.StockResponse;
import com.example.magazyn.entity.Batch;
import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.StockMovement;
import com.example.magazyn.exception.InsufficientStockException;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.BatchRepository;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StockService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public StockService(StockMovementRepository stockMovementRepository,
                        ProductRepository productRepository,
                        BatchRepository batchRepository,
                        AuditLogService auditLogService) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.auditLogService = auditLogService;
    }

    public StockMovementResponse addMovement(Long productId, StockMovementRequest request, String username) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (request.getQuantity() == null) {
            throw new InvalidOperationException("Quantity is required");
        }
        if (request.getType() == null) {
            throw new InvalidOperationException("Movement type is required");
        }

        Long batchId = request.getBatchId();

        if (request.getType() == MovementType.WYDANIE && batchId == null) {
            // FIFO: auto-deduct from oldest batches
            return addMovementFIFO(product, request, username);
        }

        return addMovementSingle(product, request, username);
    }

    private StockMovementResponse addMovementSingle(Product product, StockMovementRequest request, String username) {
        Long requestBatchId = request.getBatchId();
        Long batchId = requestBatchId;  // may be reassigned if a new batch is created

        switch (request.getType()) {
            case PRZYJECIE:
                if (request.getQuantity() <= 0) {
                    throw new InvalidOperationException("Quantity must be positive for PRZYJECIE");
                }
                product.setQuantity(product.getQuantity() + request.getQuantity());

                if (batchId != null) {
                    // When batchId is explicitly provided for PRZYJECIE, add to that batch
                    Batch batch = batchRepository.findByIdForUpdate(batchId)
                            .orElseThrow(() -> new ResourceNotFoundException("Batch", requestBatchId));
                    if (!batch.getProduct().getId().equals(product.getId())) {
                        throw new InvalidOperationException("Batch does not belong to this product");
                    }
                    batch.setQuantity(batch.getQuantity() + request.getQuantity());
                    batchRepository.save(batch);
                } else if (request.getLotNumber() != null && !request.getLotNumber().isBlank()) {
                    // Create new batch when lotNumber is provided
                    Batch newBatch = new Batch();
                    newBatch.setProduct(product);
                    newBatch.setLotNumber(request.getLotNumber().trim());
                    newBatch.setExpiryDate(request.getExpiryDate());
                    newBatch.setManufacturingDate(request.getManufacturingDate());
                    newBatch.setQuantity(request.getQuantity());
                    newBatch = batchRepository.save(newBatch);
                    batchId = newBatch.getId();
                }
                break;

            case WYDANIE:
                if (request.getQuantity() <= 0) {
                    throw new InvalidOperationException("Quantity must be positive for WYDANIE");
                }
                if (product.getQuantity() < request.getQuantity()) {
                    throw new InsufficientStockException("Insufficient stock \u2014 available: "
                            + product.getQuantity() + ", requested: " + request.getQuantity());
                }
                product.setQuantity(product.getQuantity() - request.getQuantity());

                if (batchId != null) {
                    // Deduct from a specific batch
                    Batch batch = batchRepository.findByIdForUpdate(batchId)
                            .orElseThrow(() -> new ResourceNotFoundException("Batch", requestBatchId));
                    if (!batch.getProduct().getId().equals(product.getId())) {
                        throw new InvalidOperationException("Batch does not belong to this product");
                    }
                    if (batch.getQuantity() < request.getQuantity()) {
                        throw new InsufficientStockException("Insufficient batch quantity \u2014 available: "
                                + batch.getQuantity() + ", requested: " + request.getQuantity());
                    }
                    batch.setQuantity(batch.getQuantity() - request.getQuantity());
                    batchRepository.save(batch);
                }
                break;

            case KOREKTA:
                if (request.getQuantity() < 0) {
                    throw new InvalidOperationException("Quantity must be non-negative for KOREKTA");
                }
                product.setQuantity(request.getQuantity());
                break;
        }

        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .type(request.getType())
                .quantity(request.getQuantity())
                .note(request.getNote())
                .createdBy(username)
                .batchId(batchId)
                .build();

        StockMovement saved = stockMovementRepository.save(movement);
        String action = "STOCK_" + request.getType().name();
        auditLogService.log(username, action, "StockMovement", saved.getId(),
                request.getType().name() + " productId=" + product.getId() + " qty=" + request.getQuantity()
                        + (batchId != null ? " batchId=" + batchId : "")
                        + " (note: " + (request.getNote() != null ? request.getNote() : "") + ")");
        return toResponse(saved);
    }

    /**
     * FIFO stock deduction: consumes from the oldest batches (by createdAt) until the requested quantity is met.
     * Called when WYDANIE is performed without specifying a batchId.
     * <p>
     * Each batch is locked individually with PESSIMISTIC_WRITE immediately on access
     * to prevent TOCTOU race conditions with concurrent transactions.
     * If batch stock is insufficient, falls back to product-level un-tracked stock.
     */
    private StockMovementResponse addMovementFIFO(Product product, StockMovementRequest request, String username) {
        if (request.getQuantity() <= 0) {
            throw new InvalidOperationException("Quantity must be positive for WYDANIE");
        }

        if (product.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock \u2014 available: "
                    + product.getQuantity() + ", requested: " + request.getQuantity());
        }

        List<Batch> batches = batchRepository.findByProductIdOrderByCreatedAtAsc(product.getId());
        int remaining = request.getQuantity();

        // Phase 1: deduct from batches — lock each batch immediately as we access it
        for (Batch batch : batches) {
            if (remaining <= 0) break;
            if (batch.getQuantity() <= 0) continue;

            Batch lockedBatch = batchRepository.findByIdForUpdate(batch.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch", batch.getId()));

            if (lockedBatch.getQuantity() <= 0) continue;

            int deductFromThis = Math.min(remaining, lockedBatch.getQuantity());
            lockedBatch.setQuantity(lockedBatch.getQuantity() - deductFromThis);
            batchRepository.save(lockedBatch);
            remaining -= deductFromThis;
        }

        // Phase 2: remaining quantity comes from un-tracked product stock
        int deducted = request.getQuantity() - remaining;
        if (deducted < request.getQuantity() && product.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock \u2014 available: "
                    + product.getQuantity() + ", requested: " + request.getQuantity());
        }

        product.setQuantity(product.getQuantity() - request.getQuantity());
        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .type(request.getType())
                .quantity(request.getQuantity())
                .note(request.getNote())
                .createdBy(username)
                .batchId(null)
                .build();

        String action = remaining == 0 ? "STOCK_WYDANIE_FIFO" : "STOCK_WYDANIE_FIFO_PARTIAL";
        StockMovement saved = stockMovementRepository.save(movement);
        auditLogService.log(username, action, "StockMovement", saved.getId(),
                "WYDANIE FIFO productId=" + product.getId() + " qty=" + request.getQuantity()
                        + " batchDeducted=" + deducted + " (note: "
                        + (request.getNote() != null ? request.getNote() : "") + ")");
        return toResponse(saved);
    }

    /**
     * Dedicated receipt method for PZ confirmations: creates or updates a batch for each item.
     * When a batch with the same lotNumber exists for this product, adds to it.
     * Otherwise creates a new batch.
     */
    public StockMovementResponse addMovementWithBatchCreate(Long productId, StockMovementRequest request,
                                                             String username, String lotNumber,
                                                         java.time.LocalDate expiryDate,
                                                         java.time.LocalDate manufacturingDate) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidOperationException("Quantity must be positive for PRZYJECIE");
        }
        if (request.getType() != MovementType.PRZYJECIE) {
            throw new InvalidOperationException("Batch creation is only supported for PRZYJECIE");
        }

        product.setQuantity(product.getQuantity() + request.getQuantity());
        productRepository.save(product);

        // Find or create batch
        List<Batch> existingBatches = batchRepository.findByProductIdOrderByCreatedAtAsc(productId);
        Batch batch = existingBatches.stream()
                .filter(b -> b.getLotNumber().equals(lotNumber))
                .findFirst()
                .orElse(null);

        if (batch != null) {
            batch.setQuantity(batch.getQuantity() + request.getQuantity());
            if (expiryDate != null) batch.setExpiryDate(expiryDate);
            if (manufacturingDate != null) batch.setManufacturingDate(manufacturingDate);
        } else {
            batch = Batch.builder()
                    .product(product)
                    .lotNumber(lotNumber)
                    .expiryDate(expiryDate)
                    .manufacturingDate(manufacturingDate)
                    .quantity(request.getQuantity())
                    .locationId(product.getLocationId())
                    .build();
        }
        Batch savedBatch = batchRepository.save(batch);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .type(MovementType.PRZYJECIE)
                .quantity(request.getQuantity())
                .note(request.getNote())
                .createdBy(username)
                .batchId(savedBatch.getId())
                .build();

        StockMovement saved = stockMovementRepository.save(movement);
        auditLogService.log(username, "STOCK_PRZYJECIE_BATCH", "StockMovement", saved.getId(),
                "PRZYJECIE productId=" + productId + " qty=" + request.getQuantity()
                        + " lotNumber=" + lotNumber + " batchId=" + savedBatch.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovements(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toResponse);
    }

    /** Backward-compatible wrapper — returns all movements for a product */
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements(Long productId) {
        return getMovements(productId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return new StockResponse(product.getId(), product.getName(), product.getSku(), product.getQuantity());
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getType(),
                movement.getQuantity(),
                movement.getNote(),
                movement.getCreatedAt(),
                movement.getCreatedBy(),
                movement.getBatchId()
        );
    }
}
