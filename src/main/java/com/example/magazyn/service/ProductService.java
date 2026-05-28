package com.example.magazyn.service;

import com.example.magazyn.dto.AssignLocationRequest;
import com.example.magazyn.dto.CreateProductRequest;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.dto.UpdateProductRequest;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.ProductRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final MeterRegistry meterRegistry;
    private final AuditLogService auditLogService;
    private final ReservationService reservationService;

    @Autowired
    public ProductService(ProductRepository productRepository, MeterRegistry meterRegistry,
                          AuditLogService auditLogService,
                          ReservationService reservationService) {
        this.productRepository = productRepository;
        this.meterRegistry = meterRegistry;
        this.auditLogService = auditLogService;
        this.reservationService = reservationService;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable, String search) {
        meterRegistry.counter("products.queries.count").increment();
        if (search == null || search.isBlank()) {
            return productRepository.findAll(pageable)
                    .map(this::toResponse);
        }
        return productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(search, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(this::toResponse);
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        meterRegistry.counter("products.created.count").increment();
        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new RuntimeException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .unit(request.getUnit())
                .quantity(0)
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .minQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 0)
                .build();

        Product saved = productRepository.save(product);
        auditLogService.log(currentUsername(), "CREATE_PRODUCT", "Product", saved.getId(),
                "Created product: " + saved.getName() + " (SKU: " + saved.getSku() + ")");
        return toResponse(saved);
    }

    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getSku() != null) {
            if (!existing.getSku().equals(request.getSku())
                    && productRepository.findBySku(request.getSku()).isPresent()) {
                throw new RuntimeException("Product with SKU '" + request.getSku() + "' already exists");
            }
            existing.setSku(request.getSku());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getUnit() != null) {
            existing.setUnit(request.getUnit());
        }
        if (request.getPrice() != null) {
            existing.setPrice(request.getPrice());
        }
        if (request.getMinQuantity() != null) {
            existing.setMinQuantity(request.getMinQuantity());
        }

        Product saved = productRepository.save(existing);
        auditLogService.log(currentUsername(), "UPDATE_PRODUCT", "Product", saved.getId(),
                "Updated product: " + saved.getName() + " (SKU: " + saved.getSku() + ")");
        return toResponse(saved);
    }

    public void deleteProduct(Long id) {
        meterRegistry.counter("products.deleted.count").increment();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        auditLogService.log(currentUsername(), "DELETE_PRODUCT", "Product", id,
                "Deleted product: " + product.getName() + " (SKU: " + product.getSku() + ")");
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByLocation(Long locationId) {
        return productRepository.findByLocationId(locationId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse assignLocation(Long productId, AssignLocationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        product.setLocationId(request.getLocationId());
        Product saved = productRepository.save(product);
        auditLogService.log(currentUsername(), "ASSIGN_LOCATION", "Product", productId,
                "Assigned locationId=" + request.getLocationId() + " to product: " + product.getName());
        return toResponse(saved);
    }

    private ProductResponse toResponse(Product product) {
        int reservedQuantity = reservationService.getActiveReservedQuantity(product.getId());
        int availableQuantity = Math.max(product.getQuantity() - reservedQuantity, 0);

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getUnit(),
                product.getQuantity(),
                product.getPrice(),
                product.getMinQuantity(),
                product.getLocationId(),
                product.getCreatedAt(),
                reservedQuantity,
                availableQuantity
        );
    }
}
