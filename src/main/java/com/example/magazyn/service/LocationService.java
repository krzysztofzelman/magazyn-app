package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.LocationResponse;
import com.example.magazyn.dto.LocationStockResponse;
import com.example.magazyn.dto.LocationTreeNode;
import com.example.magazyn.dto.TransferRequest;
import com.example.magazyn.dto.TransferResponse;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationStock;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.exception.InsufficientStockException;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.LocationStockRepository;
import com.example.magazyn.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationStockRepository locationStockRepository;
    private final ProductRepository productRepository;
    private final BarcodeService barcodeService;
    private final AuditLogService auditLogService;

    @Autowired
    public LocationService(LocationRepository locationRepository,
                           LocationStockRepository locationStockRepository,
                           ProductRepository productRepository,
                           BarcodeService barcodeService,
                           AuditLogService auditLogService) {
        this.locationRepository = locationRepository;
        this.locationStockRepository = locationStockRepository;
        this.productRepository = productRepository;
        this.barcodeService = barcodeService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .sorted(Comparator.comparing(Location::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<LocationResponse> getLocationById(Long id) {
        return locationRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<LocationResponse> getLocationByBarcode(String barcode) {
        return locationRepository.findByBarcode(barcode)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<LocationTreeNode> getLocationTree() {
        List<Location> all = locationRepository.findAll();
        Map<Long, List<Location>> childrenByParent = all.stream()
                .filter(l -> l.getParentId() != null)
                .collect(Collectors.groupingBy(Location::getParentId));

        return all.stream()
                .filter(l -> l.getParentId() == null)
                .sorted(Comparator.comparing(Location::getId))
                .map(l -> buildTreeNode(l, childrenByParent))
                .toList();
    }

    private LocationTreeNode buildTreeNode(Location location, Map<Long, List<Location>> childrenByParent) {
        LocationTreeNode node = new LocationTreeNode();
        node.setId(location.getId());
        node.setCode(location.getCode());
        node.setName(location.getName());
        node.setType(location.getType().name());
        node.setDescription(location.getDescription());
        node.setBarcode(location.getBarcode());
        node.setCapacity(location.getCapacity());
        node.setOccupied(location.getOccupied());
        node.setZone(location.getZone());
        node.setRack(location.getRack());
        node.setShelf(location.getShelf());
        node.setIsActive(location.getIsActive());

        List<Location> children = new ArrayList<>(childrenByParent.getOrDefault(location.getId(), List.of()));
        children.sort(Comparator.comparing(Location::getId));
        for (Location child : children) {
            node.getChildren().add(buildTreeNode(child, childrenByParent));
        }
        return node;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getChildren(Long parentId) {
        return locationRepository.findByParentId(parentId).stream()
                .sorted(Comparator.comparing(Location::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationStockResponse> getLocationStock(Long locationId) {
        if (!locationRepository.existsByIdAndTenantId(locationId, TenantContext.getTenantId())) {
            throw new ResourceNotFoundException("Location", locationId);
        }

        return locationStockRepository.findByLocationId(locationId).stream()
                .map(this::toStockResponse)
                .toList();
    }

    public LocationResponse createLocation(com.example.magazyn.dto.LocationRequest request) {
        validateParent(request.getParentId());

        // Auto-generate code from zone/rack/shelf if code not provided
        String code = request.getCode();
        if ((code == null || code.isBlank()) && request.getZone() != null && request.getRack() != null && request.getShelf() != null) {
            code = request.getZone() + "-" + request.getRack() + "-" + request.getShelf();
        } else if (code == null || code.isBlank()) {
            throw new InvalidOperationException("Kod lokalizacji jest wymagany, gdy nie podano strefy/regału/półki");
        }

        Location location = Location.builder()
                .code(code)
                .name(request.getName())
                .type(request.getType())
                .parentId(request.getParentId())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .zone(request.getZone())
                .rack(request.getRack())
                .shelf(request.getShelf())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .occupied(0)
                .build();

        Location saved = locationRepository.save(location);
        return toResponse(saved);
    }

    public LocationResponse updateLocation(Long id, com.example.magazyn.dto.LocationRequest request) {
        Location existing = locationRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", id));

        validateParent(request.getParentId());

        // Auto-generate code from zone/rack/shelf if code not provided and all parts are present
        if (request.getCode() != null && !request.getCode().isBlank()) {
            existing.setCode(request.getCode());
        } else if (request.getZone() != null && request.getRack() != null && request.getShelf() != null) {
            existing.setCode(request.getZone() + "-" + request.getRack() + "-" + request.getShelf());
        }
        // If neither code nor structured parts provided, keep existing code

        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getType() != null) {
            existing.setType(request.getType());
        }
        existing.setParentId(request.getParentId());
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getCapacity() != null) {
            existing.setCapacity(request.getCapacity());
        }
        if (request.getZone() != null) {
            existing.setZone(request.getZone());
        }
        if (request.getRack() != null) {
            existing.setRack(request.getRack());
        }
        if (request.getShelf() != null) {
            existing.setShelf(request.getShelf());
        }
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }

        Location saved = locationRepository.save(existing);
        return toResponse(saved);
    }

    public void deleteLocation(Long id) {
        Location location = locationRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", id));

        if (locationRepository.existsByParentId(id)) {
            throw new InvalidOperationException("Cannot delete location with children. Remove children first.");
        }

        locationRepository.delete(location);
    }

    public byte[] getBarcodeImage(Long locationId) {
        Location location = locationRepository.findByIdAndTenantId(locationId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", locationId));

        String barcodeText = location.getBarcode();
        if (barcodeText == null || barcodeText.isBlank()) {
            throw new InvalidOperationException("Location has no barcode assigned");
        }

        return barcodeService.generateBarcodeImage(barcodeText);
    }

    public byte[] getQrImage(Long locationId) {
        Location location = locationRepository.findByIdAndTenantId(locationId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", locationId));

        String qrData = location.getQrData();
        if (qrData == null || qrData.isBlank()) {
            throw new InvalidOperationException("Location has no QR data assigned");
        }

        return barcodeService.generateQrImage(qrData);
    }

    public TransferResponse transferStock(TransferRequest request, String username) {
        Location fromLocation = locationRepository.findByBarcode(request.getFromBarcode())
                .orElseThrow(() -> new ResourceNotFoundException("Source location with barcode " + request.getFromBarcode()));

        Location toLocation = locationRepository.findByBarcode(request.getToBarcode())
                .orElseThrow(() -> new ResourceNotFoundException("Destination location with barcode " + request.getToBarcode()));

        Product product = productRepository.findByIdAndTenantId(request.getProductId(), TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());

        // Check source stock sufficiency (available = quantity - reserved)
        LocationStock fromStock = locationStockRepository
                .findByLocationIdAndProductIdWithLock(fromLocation.getId(), product.getId())
                .orElseThrow(() -> new InvalidOperationException(
                        "Product " + product.getName() + " is not stored in source location " + fromLocation.getCode()));

        BigDecimal available = fromStock.getQuantity().subtract(fromStock.getReservedQuantity());
        if (available.compareTo(quantity) < 0) {
            throw new InsufficientStockException(
                    "Insufficient stock in " + fromLocation.getCode()
                    + ": available " + available + ", requested " + quantity);
        }

        // Decrease from-stock
        BigDecimal newFromQuantity = fromStock.getQuantity().subtract(quantity);
        if (newFromQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            locationStockRepository.delete(fromStock);
        } else {
            fromStock.setQuantity(newFromQuantity);
            fromStock.setUpdatedAt(LocalDateTime.now());
            locationStockRepository.save(fromStock);
        }

        // Increase to-stock (create if not exists)
        LocationStock toStock = locationStockRepository
                .findByLocationIdAndProductIdWithLock(toLocation.getId(), product.getId())
                .orElse(LocationStock.builder()
                        .locationId(toLocation.getId())
                        .productId(product.getId())
                        .reservedQuantity(BigDecimal.ZERO)
                        .build());

        toStock.setQuantity(toStock.getQuantity().add(quantity));
        toStock.setUpdatedAt(LocalDateTime.now());
        locationStockRepository.save(toStock);

        // Update occupied for both locations
        updateLocationOccupied(fromLocation.getId());
        updateLocationOccupied(toLocation.getId());

        auditLogService.log(username, "STOCK_TRANSFER", "LocationStock", null,
                "from=" + fromLocation.getCode() + " to=" + toLocation.getCode()
                + " product=" + product.getName() + " quantity=" + quantity);

        return new TransferResponse(
                fromLocation.getCode(), fromLocation.getName(),
                toLocation.getCode(), toLocation.getName(),
                product.getId(), product.getName(),
                request.getQuantity(), LocalDateTime.now()
        );
    }

    private void updateLocationOccupied(Long locationId) {
        List<LocationStock> stocks = locationStockRepository.findByLocationId(locationId);
        int totalOccupied = stocks.stream()
                .map(s -> s.getQuantity().setScale(0, RoundingMode.UP).intValue())
                .reduce(0, Integer::sum);

        locationRepository.findById(locationId).ifPresent(location -> {
            location.setOccupied(totalOccupied);
            locationRepository.save(location);
        });
    }

    private void validateParent(Long parentId) {
        if (parentId != null && !locationRepository.existsByIdAndTenantId(parentId, TenantContext.getTenantId())) {
            throw new ResourceNotFoundException("Parent location", parentId);
        }
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getType().name(),
                location.getParentId(),
                location.getDescription(),
                location.getBarcode(),
                location.getQrData(),
                location.getCapacity(),
                location.getOccupied(),
                location.getZone(),
                location.getRack(),
                location.getShelf(),
                location.getIsActive()
        );
    }

    private LocationStockResponse toStockResponse(LocationStock stock) {
        BigDecimal available = stock.getQuantity().subtract(stock.getReservedQuantity());

        String productName = "";
        String productSku = "";
        String productUnit = "";
        Optional<Product> product = productRepository.findById(stock.getProductId());
        if (product.isPresent()) {
            productName = product.get().getName();
            productSku = product.get().getSku();
            productUnit = product.get().getUnit();
        }

        return new LocationStockResponse(
                stock.getId(),
                stock.getLocationId(),
                stock.getProductId(),
                productName,
                productSku,
                productUnit,
                stock.getQuantity(),
                stock.getReservedQuantity(),
                available,
                stock.getUpdatedAt()
        );
    }
}
