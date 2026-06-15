package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.InventoryItemResponse;
import com.example.magazyn.dto.InventoryReportResponse;
import com.example.magazyn.dto.InventoryScanRequest;
import com.example.magazyn.dto.InventorySessionRequest;
import com.example.magazyn.dto.InventorySessionResponse;
import com.example.magazyn.entity.InventoryItem;
import com.example.magazyn.entity.InventorySession;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationStock;
import com.example.magazyn.entity.Product;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.InventoryItemRepository;
import com.example.magazyn.repository.InventorySessionRepository;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.LocationStockRepository;
import com.example.magazyn.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final InventorySessionRepository sessionRepository;
    private final InventoryItemRepository itemRepository;
    private final LocationRepository locationRepository;
    private final LocationStockRepository locationStockRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public InventoryService(InventorySessionRepository sessionRepository,
                            InventoryItemRepository itemRepository,
                            LocationRepository locationRepository,
                            LocationStockRepository locationStockRepository,
                            ProductRepository productRepository,
                            AuditLogService auditLogService) {
        this.sessionRepository = sessionRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.locationStockRepository = locationStockRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    public InventorySessionResponse createSession(InventorySessionRequest request, String username) {
        Long tenantId = TenantContext.getTenantId();
        InventorySession session = InventorySession.builder()
                .name(request.getName())
                .createdBy(username)
                .status("OPEN")
                .warehouseId(request.getWarehouseId())
                .build();
        session = sessionRepository.save(session);

        // Auto-populate expected_quantity from location_stock for all locations under warehouse
        List<Long> locationIds = new ArrayList<>();
        if (request.getWarehouseId() != null) {
            collectDescendantLocationIds(request.getWarehouseId(), locationIds, tenantId);
        }

        List<InventoryItem> items = new ArrayList<>();
        if (!locationIds.isEmpty()) {
            for (Long locId : locationIds) {
                List<LocationStock> stocks = locationStockRepository.findByLocationIdAndTenantId(locId, tenantId);
                for (LocationStock stock : stocks) {
                    items.add(InventoryItem.builder()
                            .sessionId(session.getId())
                            .locationId(locId)
                            .productId(stock.getProductId())
                            .expectedQuantity(stock.getQuantity())
                            .build());
                }
            }
        }

        // Also add location_stock entries for locations not in the warehouse tree
        // when no warehouseId is provided, add all location_stock
        if (request.getWarehouseId() == null) {
            List<LocationStock> allStocks = locationStockRepository.findByTenantId(tenantId);
            for (LocationStock stock : allStocks) {
                items.add(InventoryItem.builder()
                        .sessionId(session.getId())
                        .locationId(stock.getLocationId())
                        .productId(stock.getProductId())
                        .expectedQuantity(stock.getQuantity())
                        .build());
            }
        }

        if (!items.isEmpty()) {
            itemRepository.saveAll(items);
        }

        auditLogService.log(username, "INVENTORY_CREATE", "InventorySession", session.getId(),
                "name=" + request.getName() + " warehouseId=" + request.getWarehouseId()
                + " items=" + items.size());

        return toSessionResponse(session, items.size());
    }

    private void collectDescendantLocationIds(Long parentId, List<Long> result, Long tenantId) {
        List<Location> children = locationRepository.findByParentIdAndTenantId(parentId, tenantId);
        for (Location child : children) {
            result.add(child.getId());
            collectDescendantLocationIds(child.getId(), result, tenantId);
        }
    }

    @Transactional(readOnly = true)
    public List<InventorySessionResponse> getAllSessions() {
        Long tenantId = TenantContext.getTenantId();
        List<InventorySession> sessions = sessionRepository.findAllByTenantId(tenantId);
        return sessions.stream()
                .map(session -> {
                    List<InventoryItem> items = itemRepository.findBySessionIdAndTenantId(session.getId(), tenantId);
                    return toSessionResponse(session, items.size());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventorySessionResponse getSession(Long id) {
        Long tenantId = TenantContext.getTenantId();
        InventorySession session = sessionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("InventorySession", id));
        List<InventoryItem> items = itemRepository.findBySessionIdAndTenantId(id, tenantId);
        return toSessionResponse(session, items.size());
    }

    public InventoryItemResponse scan(Long sessionId, InventoryScanRequest request, String username) {
        Long tenantId = TenantContext.getTenantId();
        InventorySession session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("InventorySession", sessionId));

        if (!"OPEN".equals(session.getStatus())) {
            throw new InvalidOperationException("Cannot scan in a closed inventory session");
        }

        // Find location by barcode
        Location location = locationRepository.findByBarcodeAndTenantId(request.getLocationBarcode(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Location with barcode " + request.getLocationBarcode()));

        // Find product by barcode or SKU
        Product product = findProductByBarcodeOrSku(request.getProductBarcode(), tenantId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with barcode/SKU " + request.getProductBarcode());
        }

        // Look up expected quantity from existing inventory item
        BigDecimal expectedQuantity = itemRepository
                .findBySessionIdAndLocationIdAndProductIdAndTenantId(sessionId, location.getId(), product.getId(), tenantId)
                .map(InventoryItem::getExpectedQuantity)
                .orElse(BigDecimal.ZERO);

        // Create or update inventory item
        InventoryItem item = itemRepository
                .findBySessionIdAndLocationIdAndProductIdAndTenantId(sessionId, location.getId(), product.getId(), tenantId)
                .orElse(InventoryItem.builder()
                        .sessionId(sessionId)
                        .locationId(location.getId())
                        .productId(product.getId())
                        .expectedQuantity(expectedQuantity)
                        .build());

        item.setCountedQuantity(request.getQuantity() != null
                ? BigDecimal.valueOf(request.getQuantity()) : BigDecimal.ZERO);
        item.setScannedAt(LocalDateTime.now());
        item.setScannedBy(username);
        item = itemRepository.save(item);

        auditLogService.log(username, "INVENTORY_SCAN", "InventoryItem", item.getId(),
                "session=" + sessionId + " location=" + location.getCode()
                + " product=" + product.getName() + " counted=" + request.getQuantity());

        return toItemResponse(item, location.getCode(), product);
    }

    private Product findProductByBarcodeOrSku(String barcodeOrSku, Long tenantId) {
        // Try to parse as "PROD-{id}"
        if (barcodeOrSku != null && barcodeOrSku.startsWith("PROD-")) {
            try {
                Long id = Long.parseLong(barcodeOrSku.substring(5));
                return productRepository.findByIdAndTenantId(id, tenantId).orElse(null);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }

        // Try by barcode
        return productRepository.findByBarcodeAndTenantId(barcodeOrSku, tenantId)
                .orElseGet(() -> productRepository.findBySkuAndTenantId(barcodeOrSku, tenantId).orElse(null));
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse getReport(Long sessionId) {
        Long tenantId = TenantContext.getTenantId();
        InventorySession session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("InventorySession", sessionId));

        List<InventoryItem> items = itemRepository.findBySessionIdAndTenantId(sessionId, tenantId);

        // Batch-load locations and products to avoid N+1 in toItemResponseWithLookup
        Map<Long, String> locationCodeMap = new HashMap<>();
        Map<Long, Product> productMap = new HashMap<>();
        for (InventoryItem item : items) {
            if (item.getLocationId() != null) {
                locationCodeMap.put(item.getLocationId(), null);
            }
            productMap.put(item.getProductId(), null);
        }
        if (!locationCodeMap.isEmpty()) {
            locationRepository.findAllById(new ArrayList<>(locationCodeMap.keySet()))
                    .forEach(loc -> locationCodeMap.put(loc.getId(), loc.getCode()));
        }
        if (!productMap.isEmpty()) {
            productRepository.findAllById(new ArrayList<>(productMap.keySet()))
                    .forEach(prod -> productMap.put(prod.getId(), prod));
        }

        List<InventoryItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponseWithLookup(item, locationCodeMap, productMap))
                .collect(Collectors.toList());

        BigDecimal totalExpected = items.stream()
                .map(InventoryItem::getExpectedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCounted = items.stream()
                .filter(i -> i.getCountedQuantity() != null)
                .map(InventoryItem::getCountedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDifference = totalCounted.subtract(totalExpected);

        return new InventoryReportResponse(
                session.getId(), session.getName(), session.getStatus(),
                itemResponses, totalExpected, totalCounted, totalDifference);
    }

    public InventorySessionResponse closeSession(Long sessionId, String username) {
        Long tenantId = TenantContext.getTenantId();
        InventorySession session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("InventorySession", sessionId));

        if (!"OPEN".equals(session.getStatus())) {
            throw new InvalidOperationException("Session is already " + session.getStatus());
        }

        List<InventoryItem> items = itemRepository.findBySessionIdAndTenantId(sessionId, tenantId);

        // Update location_stock and location.occupied for each counted item
        for (InventoryItem item : items) {
            if (item.getCountedQuantity() == null || item.getLocationId() == null) {
                continue;
            }

            LocationStock stock = locationStockRepository
                    .findByLocationIdAndProductIdAndTenantId(item.getLocationId(), item.getProductId(), tenantId)
                    .orElse(null);

            if (stock != null) {
                if (item.getCountedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    locationStockRepository.delete(stock);
                } else {
                    stock.setQuantity(item.getCountedQuantity());
                    stock.setUpdatedAt(LocalDateTime.now());
                    locationStockRepository.save(stock);
                }
            } else if (item.getCountedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                stock = LocationStock.builder()
                        .locationId(item.getLocationId())
                        .productId(item.getProductId())
                        .quantity(item.getCountedQuantity())
                        .reservedQuantity(BigDecimal.ZERO)
                        .updatedAt(LocalDateTime.now())
                        .build();
                locationStockRepository.save(stock);
            }

            // Update location.occupied
            updateLocationOccupied(item.getLocationId(), tenantId);
        }

        session.setStatus("CLOSED");
        sessionRepository.save(session);

        auditLogService.log(username, "INVENTORY_CLOSE", "InventorySession", sessionId,
                "name=" + session.getName() + " items=" + items.size());

        return toSessionResponse(session, items.size());
    }

    private void updateLocationOccupied(Long locationId, Long tenantId) {
        List<LocationStock> stocks = locationStockRepository.findByLocationIdAndTenantId(locationId, tenantId);
        int totalOccupied = stocks.stream()
                .map(s -> s.getQuantity().setScale(0, RoundingMode.UP).intValue())
                .reduce(0, Integer::sum);

        locationRepository.findByIdAndTenantId(locationId, tenantId).ifPresent(location -> {
            location.setOccupied(totalOccupied);
            locationRepository.save(location);
        });
    }

    private InventorySessionResponse toSessionResponse(InventorySession session, int itemCount) {
        Long tenantId = TenantContext.getTenantId();
        String warehouseName = null;
        if (session.getWarehouseId() != null) {
            warehouseName = locationRepository.findByIdAndTenantId(session.getWarehouseId(), tenantId)
                    .map(Location::getName)
                    .orElse(null);
        }

        return new InventorySessionResponse(
                session.getId(), session.getName(), session.getCreatedAt(),
                session.getCreatedBy(), session.getStatus(),
                session.getWarehouseId(), warehouseName, itemCount);
    }

    private InventoryItemResponse toItemResponse(InventoryItem item, String locationCode, Product product) {
        BigDecimal difference = item.getCountedQuantity() != null
                ? item.getCountedQuantity().subtract(item.getExpectedQuantity())
                : BigDecimal.ZERO;

        return new InventoryItemResponse(
                item.getId(), item.getSessionId(), item.getLocationId(), locationCode,
                product.getId(), product.getName(), product.getSku(),
                item.getExpectedQuantity(), item.getCountedQuantity(),
                difference, item.getScannedAt(), item.getScannedBy());
    }

    private InventoryItemResponse toItemResponseWithLookup(InventoryItem item,
                                                            Map<Long, String> locationCodeMap,
                                                            Map<Long, Product> productMap) {
        String locationCode = item.getLocationId() != null
                ? locationCodeMap.get(item.getLocationId())
                : null;

        Product product = productMap.get(item.getProductId());
        String productName = product != null ? product.getName() : "Deleted";
        String productSku = product != null ? product.getSku() : "-";

        BigDecimal difference = item.getCountedQuantity() != null
                ? item.getCountedQuantity().subtract(item.getExpectedQuantity())
                : BigDecimal.ZERO;

        return new InventoryItemResponse(
                item.getId(), item.getSessionId(), item.getLocationId(), locationCode,
                item.getProductId(), productName, productSku,
                item.getExpectedQuantity(), item.getCountedQuantity(),
                difference, item.getScannedAt(), item.getScannedBy());
    }
}
