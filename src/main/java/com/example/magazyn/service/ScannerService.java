package com.example.magazyn.service;

import com.example.magazyn.dto.ScannerResultResponse;
import com.example.magazyn.dto.ScannerResultResponse.BatchInfo;
import com.example.magazyn.dto.ScannerResultResponse.LastMovementInfo;
import com.example.magazyn.dto.ScannerResultResponse.LocationInfo;
import com.example.magazyn.dto.StockMovementRequest;
import com.example.magazyn.dto.StockMovementResponse;
import com.example.magazyn.entity.Batch;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.exception.InsufficientStockException;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.BatchRepository;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.StockMovementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ScannerService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final LocationRepository locationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockService stockService;
    private final ReservationService reservationService;
    private final AuditLogService auditLogService;

    public ScannerService(ProductRepository productRepository,
                          BatchRepository batchRepository,
                          LocationRepository locationRepository,
                          StockMovementRepository stockMovementRepository,
                          StockService stockService,
                          ReservationService reservationService,
                          AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.locationRepository = locationRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockService = stockService;
        this.reservationService = reservationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public ScannerResultResponse lookupByCode(String code, String username) {
        Product product = productRepository.findBySku(code)
                .orElseGet(() -> productRepository.findByBarcode(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Product with code: " + code)));

        auditLogService.log(username, "SCAN_LOOKUP", "Product", product.getId(),
                "Scanned code=" + code + " matched product=" + product.getSku());

        return buildResult(product);
    }

    public ScannerResultResponse quickReceive(Long productId, Integer quantity,
                                               String lotNumber, LocalDate expiryDate,
                                               LocalDate manufacturingDate, Long locationId,
                                               String username) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (quantity == null || quantity <= 0) {
            throw new InvalidOperationException("Quantity must be positive for quick receive");
        }

        StockMovementRequest request = new StockMovementRequest();
        request.setType(MovementType.PRZYJECIE);
        request.setQuantity(quantity);
        request.setNote("Quick receive (scanner)");

        StockMovementResponse movement;

        // If product tracks expiry and has lot number, use batch-aware path
        if (Boolean.TRUE.equals(product.getTrackExpiry()) && lotNumber != null && !lotNumber.isBlank()) {
            movement = stockService.addMovementWithBatchCreate(productId, request, username,
                    lotNumber, expiryDate, manufacturingDate);
        } else if (Boolean.TRUE.equals(product.getTrackExpiry()) && lotNumber == null) {
            // trackExpiry enabled but no lotNumber — still need a default batch
            throw new InvalidOperationException("Lot number is required for products with expiry tracking");
        } else {
            movement = stockService.addMovement(productId, request, username);
        }

        // Optionally update location
        if (locationId != null) {
            product.setLocationId(locationId);
            productRepository.save(product);
        }

        auditLogService.log(username, "SCAN_QUICK_RECEIVE", "StockMovement", movement.getId(),
                "Quick receive productId=" + productId + " qty=" + quantity
                        + (lotNumber != null ? " lot=" + lotNumber : ""));

        return buildResult(product);
    }

    public ScannerResultResponse quickIssue(Long productId, Integer quantity,
                                             Long batchId, String note, String username) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (quantity == null || quantity <= 0) {
            throw new InvalidOperationException("Quantity must be positive for quick issue");
        }

        int reserved = reservationService.getActiveReservedQuantity(productId);
        int available = Math.max(product.getQuantity() - reserved, 0);
        if (available < quantity) {
            throw new InsufficientStockException("Insufficient available stock — available: "
                    + available + ", requested: " + quantity);
        }

        StockMovementRequest request = new StockMovementRequest();
        request.setType(MovementType.WYDANIE);
        request.setQuantity(quantity);
        request.setBatchId(batchId);
        request.setNote(note != null ? "Quick issue (scanner): " + note : "Quick issue (scanner)");

        StockMovementResponse movement = stockService.addMovement(productId, request, username);

        auditLogService.log(username, "SCAN_QUICK_ISSUE", "StockMovement", movement.getId(),
                "Quick issue productId=" + productId + " qty=" + quantity
                        + (batchId != null ? " batchId=" + batchId : ""));

        return buildResult(product);
    }

    private ScannerResultResponse buildResult(Product product) {
        int reservedQuantity = reservationService.getActiveReservedQuantity(product.getId());
        int availableQuantity = Math.max(product.getQuantity() - reservedQuantity, 0);

        // Batches
        List<Batch> batches = batchRepository.findByProductIdOrderByExpiryDateAsc(product.getId());
        List<BatchInfo> batchInfos = batches.stream()
                .filter(b -> b.getQuantity() > 0)
                .map(b -> {
                    Long daysUntilExpiry = b.getExpiryDate() != null
                            ? ChronoUnit.DAYS.between(LocalDate.now(), b.getExpiryDate())
                            : null;
                    return new BatchInfo(b.getId(), b.getLotNumber(), b.getExpiryDate(),
                            b.getQuantity(), daysUntilExpiry);
                })
                .toList();

        // Location
        LocationInfo locationInfo = Optional.ofNullable(product.getLocationId())
                .flatMap(locId -> locationRepository.findById(locId))
                .map(loc -> new LocationInfo(loc.getId(), loc.getName()))
                .orElse(null);

        // Last movement
        LastMovementInfo lastMovementInfo = stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(product.getId(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(m -> {
                    MovementType mt = null;
                    try { mt = m.getType(); } catch (Exception ignored) {}
                    return new LastMovementInfo(mt, m.getQuantity(), m.getCreatedAt(), m.getCreatedBy());
                })
                .orElse(null);

        return new ScannerResultResponse(
                product.getId(), product.getName(), product.getSku(), product.getBarcode(),
                product.getUnit(), product.getQuantity(), reservedQuantity, availableQuantity,
                product.getTrackExpiry(), batchInfos, locationInfo, lastMovementInfo
        );
    }
}
