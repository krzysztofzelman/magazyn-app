package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
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
import java.time.LocalDate;
import com.example.magazyn.exception.DuplicateResourceException;
import com.example.magazyn.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final MeterRegistry meterRegistry;
    private final AuditLogService auditLogService;
    private final ReservationService reservationService;
    private final BatchService batchService;

    @Autowired
    public ProductService(ProductRepository productRepository, MeterRegistry meterRegistry,
                          AuditLogService auditLogService,
                          ReservationService reservationService,
                          BatchService batchService) {
        this.productRepository = productRepository;
        this.meterRegistry = meterRegistry;
        this.auditLogService = auditLogService;
        this.reservationService = reservationService;
        this.batchService = batchService;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable, String search) {
        meterRegistry.counter("products.queries.count").increment();
        Long tenantId = TenantContext.getTenantId();
        Map<Long, LocalDate> nearestExpiryMap = batchService.getNearestExpiryDateByProduct();
        if (search == null || search.isBlank()) {
            return productRepository.findAllByTenantId(tenantId, pageable)
                    .map(product -> toResponse(product, nearestExpiryMap));
        }
        return productRepository.searchByNameOrSku(tenantId, search, pageable)
                .map(product -> toResponse(product, nearestExpiryMap));
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductById(Long id) {
        Map<Long, LocalDate> nearestExpiryMap = batchService.getNearestExpiryDateByProduct();
        return productRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .map(product -> toResponse(product, nearestExpiryMap));
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductBySku(String sku) {
        Long tenantId = TenantContext.getTenantId();
        Map<Long, LocalDate> nearestExpiryMap = batchService.getNearestExpiryDateByProduct();
        return productRepository.findBySkuAndTenantId(sku, tenantId)
                .map(product -> toResponse(product, nearestExpiryMap));
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductByBarcode(String barcode) {
        Long tenantId = TenantContext.getTenantId();
        Map<Long, LocalDate> nearestExpiryMap = batchService.getNearestExpiryDateByProduct();
        return productRepository.findByBarcodeAndTenantId(barcode, tenantId)
                .map(product -> toResponse(product, nearestExpiryMap));
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        meterRegistry.counter("products.created.count").increment();
        Long tenantId = TenantContext.getTenantId();
        if (productRepository.findBySkuAndTenantId(request.getSku(), tenantId).isPresent()) {
            throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .unit(request.getUnit())
                .quantity(0)
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .minQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 0)
                .trackExpiry(request.getTrackExpiry() != null ? request.getTrackExpiry() : false)
                .barcode(request.getBarcode())
                .categoryId(request.getCategoryId())
                .defaultLocationId(request.getDefaultLocationId())
                .defaultVatRate(request.getDefaultVatRate())
                .build();

        Product saved = productRepository.save(product);
        auditLogService.log(currentUsername(), "CREATE_PRODUCT", "Product", saved.getId(),
                "Created product: " + saved.getName() + " (SKU: " + saved.getSku() + ")");
        return toResponse(saved, Map.of());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Product existing = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getSku() != null) {
            if (!existing.getSku().equals(request.getSku())
                    && productRepository.findBySkuAndTenantId(request.getSku(), tenantId).isPresent()) {
                throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
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
        if (request.getTrackExpiry() != null) {
            existing.setTrackExpiry(request.getTrackExpiry());
        }
        if (request.getBarcode() != null) {
            existing.setBarcode(request.getBarcode());
        }
        if (request.getDefaultVatRate() != null) {
            existing.setDefaultVatRate(request.getDefaultVatRate());
        }

        Product saved = productRepository.save(existing);
        auditLogService.log(currentUsername(), "UPDATE_PRODUCT", "Product", saved.getId(),
                "Updated product: " + saved.getName() + " (SKU: " + saved.getSku() + ")");
        Map<Long, LocalDate> nearestExpiryMap = batchService.getNearestExpiryDateByProduct();
        return toResponse(saved, nearestExpiryMap);
    }

    public void deleteProduct(Long id) {
        meterRegistry.counter("products.deleted.count").increment();
        Product product = productRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        auditLogService.log(currentUsername(), "DELETE_PRODUCT", "Product", id,
                "Deleted product: " + product.getName() + " (SKU: " + product.getSku() + ")");
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByLocation(Long locationId) {
        Long tenantId = TenantContext.getTenantId();
        Map<Long, LocalDate> nearestExpiryMap = batchService.getNearestExpiryDateByProduct();
        return productRepository.findByLocationIdAndTenantId(locationId, tenantId).stream()
                .map(product -> toResponse(product, nearestExpiryMap))
                .toList();
    }

    public ProductResponse assignLocation(Long productId, AssignLocationRequest request) {
        Product product = productRepository.findByIdAndTenantId(productId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        product.setLocationId(request.getLocationId());
        Product saved = productRepository.save(product);
        auditLogService.log(currentUsername(), "ASSIGN_LOCATION", "Product", productId,
                "Assigned locationId=" + request.getLocationId() + " to product: " + product.getName());
        Map<Long, LocalDate> nearestExpiryMap = batchService.getNearestExpiryDateByProduct();
        return toResponse(saved, nearestExpiryMap);
    }

    private ProductResponse toResponse(Product product, Map<Long, LocalDate> nearestExpiryMap) {
        int reservedQuantity = reservationService.getActiveReservedQuantity(product.getId());
        int availableQuantity = Math.max(product.getQuantity() - reservedQuantity, 0);
        LocalDate nearestExpiry = nearestExpiryMap.get(product.getId());

        ProductResponse response = new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getDescription(),
                product.getUnit(),
                product.getQuantity(),
                product.getPrice(),
                product.getMinQuantity(),
                product.getLocationId(),
                product.getCreatedAt(),
                reservedQuantity,
                availableQuantity,
                nearestExpiry,
                product.getTrackExpiry()
        );
        response.setCategoryId(product.getCategoryId());
        response.setDefaultLocationId(product.getDefaultLocationId());
        response.setDefaultVatRate(product.getDefaultVatRate());
        return response;
    }
}
