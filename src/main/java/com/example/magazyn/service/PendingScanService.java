package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.PendingScanRequest;
import com.example.magazyn.dto.PendingScanResponse;
import com.example.magazyn.entity.PendingScan;
import com.example.magazyn.entity.Product;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.PendingScanRepository;
import com.example.magazyn.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PendingScanService {

    private static final Logger log = LoggerFactory.getLogger(PendingScanService.class);

    private final PendingScanRepository pendingScanRepository;
    private final ProductRepository productRepository;

    public PendingScanService(PendingScanRepository pendingScanRepository,
                              ProductRepository productRepository) {
        this.pendingScanRepository = pendingScanRepository;
        this.productRepository = productRepository;
    }

    public PendingScanResponse addScan(PendingScanRequest request, String username) {
        Long tenantId = TenantContext.getTenantId();
        String mode = request.getMode();
        String barcode = request.getBarcode();
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;

        log.info("addScan: mode={}, barcode={}, qty={}, user={}", mode, barcode, quantity, username);

        if (mode == null || mode.isBlank()) {
            throw new InvalidOperationException("Mode is required (PZ, WZ, TRANSFER, INVENTORY)");
        }
        if (barcode == null || barcode.isBlank()) {
            throw new InvalidOperationException("Barcode is required");
        }
        if (quantity <= 0) {
            throw new InvalidOperationException("Quantity must be positive");
        }

        // Look up product by SKU or barcode
        Product product = productRepository.findBySkuAndTenantId(barcode, tenantId)
                .orElseGet(() -> productRepository.findByBarcodeAndTenantId(barcode, tenantId)
                        .orElseThrow(() -> {
                            log.warn("addScan: no product found for barcode={}", barcode);
                            return new ResourceNotFoundException("Product with code: " + barcode);
                        }));

        PendingScan scan = PendingScan.builder()
                .mode(mode)
                .barcode(barcode)
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .productUnit(product.getUnit())
                .quantity(quantity)
                .scannedBy(username)
                .build();

        scan = pendingScanRepository.save(scan);

        log.debug("addScan: saved pendingScan id={} for product={}", scan.getId(), product.getSku());

        return toResponse(scan);
    }

    @Transactional(readOnly = true)
    public List<PendingScanResponse> listScans(String mode, String username) {
        Long tenantId = TenantContext.getTenantId();
        log.debug("listScans: mode={}, user={}", mode, username);
        return pendingScanRepository.findByModeAndScannedByAndTenantIdOrderByCreatedAtAsc(mode, username, tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void removeScan(Long id, String username) {
        Long tenantId = TenantContext.getTenantId();
        log.info("removeScan: id={}, user={}", id, username);
        pendingScanRepository.deleteByIdAndScannedByAndTenantId(id, username, tenantId);
    }

    public int clearScans(String mode, String username) {
        Long tenantId = TenantContext.getTenantId();
        log.info("clearScans: mode={}, user={}", mode, username);
        long count = pendingScanRepository.countByModeAndScannedByAndTenantId(mode, username, tenantId);
        pendingScanRepository.deleteByModeAndScannedByAndTenantId(mode, username, tenantId);
        log.debug("clearScans: removed {} pending scans for mode={}, user={}", count, mode, username);
        return (int) count;
    }

    public int confirmScans(String mode, String username) {
        Long tenantId = TenantContext.getTenantId();
        log.info("confirmScans: mode={}, user={}", mode, username);
        List<PendingScan> scans = pendingScanRepository.findByModeAndScannedByAndTenantIdOrderByCreatedAtAsc(mode, username, tenantId);

        if (scans.isEmpty()) {
            log.warn("confirmScans: no pending scans for mode={}, user={}", mode, username);
            return 0;
        }

        // For now, just log and remove — actual business logic (stock movement creation
        // based on mode) can be wired here in a future iteration.
        for (PendingScan scan : scans) {
            log.info("confirmScans: confirming scan id={}, mode={}, product={}, qty={}",
                    scan.getId(), scan.getMode(), scan.getProductSku(), scan.getQuantity());
        }

        pendingScanRepository.deleteByModeAndScannedByAndTenantId(mode, username, tenantId);
        log.info("confirmScans: confirmed {} scans for mode={}, user={}", scans.size(), mode, username);
        return scans.size();
    }

    private PendingScanResponse toResponse(PendingScan scan) {
        return new PendingScanResponse(
                scan.getId(),
                scan.getMode(),
                scan.getBarcode(),
                scan.getProductId(),
                scan.getProductName(),
                scan.getProductSku(),
                scan.getProductUnit(),
                scan.getQuantity(),
                scan.getScannedBy(),
                scan.getCreatedAt()
        );
    }
}
