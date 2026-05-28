package com.example.magazyn.service;

import com.example.magazyn.dto.StockMovementRequest;
import com.example.magazyn.dto.StockMovementResponse;
import com.example.magazyn.dto.StockResponse;
import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.StockMovement;
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
    private final AuditLogService auditLogService;

    @Autowired
    public StockService(StockMovementRepository stockMovementRepository,
                        ProductRepository productRepository,
                        AuditLogService auditLogService) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    public StockMovementResponse addMovement(Long productId, StockMovementRequest request, String username) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        if (request.getQuantity() == null) {
            throw new RuntimeException("Quantity is required");
        }
        if (request.getType() == null) {
            throw new RuntimeException("Movement type is required");
        }

        switch (request.getType()) {
            case PRZYJECIE:
                if (request.getQuantity() <= 0) {
                    throw new RuntimeException("Quantity must be positive for PRZYJECIE");
                }
                product.setQuantity(product.getQuantity() + request.getQuantity());
                break;
            case WYDANIE:
                if (request.getQuantity() <= 0) {
                    throw new RuntimeException("Quantity must be positive for WYDANIE");
                }
                if (product.getQuantity() < request.getQuantity()) {
                    throw new RuntimeException("Insufficient stock — available: "
                            + product.getQuantity() + ", requested: " + request.getQuantity());
                }
                product.setQuantity(product.getQuantity() - request.getQuantity());
                break;
            case KOREKTA:
                if (request.getQuantity() < 0) {
                    throw new RuntimeException("Quantity must be non-negative for KOREKTA");
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
                .build();

        StockMovement saved = stockMovementRepository.save(movement);
        String action = "STOCK_" + request.getType().name();
        auditLogService.log(username, action, "StockMovement", saved.getId(),
                request.getType().name() + " productId=" + productId + " qty=" + request.getQuantity()
                        + " (note: " + (request.getNote() != null ? request.getNote() : "") + ")");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovements(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found with id: " + productId);
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
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
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
                movement.getCreatedBy()
        );
    }
}
