package com.example.magazyn.dto;

import com.example.magazyn.entity.MovementType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ScannerResultResponse {

    private Long productId;
    private String name;
    private String sku;
    private String barcode;
    private String unit;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Boolean trackExpiry;
    private List<BatchInfo> batches;
    private LocationInfo location;
    private LastMovementInfo lastMovement;

    public ScannerResultResponse() {}

    public ScannerResultResponse(Long productId, String name, String sku, String barcode,
                                 String unit, Integer quantity, Integer reservedQuantity,
                                 Integer availableQuantity, Boolean trackExpiry,
                                 List<BatchInfo> batches, LocationInfo location,
                                 LastMovementInfo lastMovement) {
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.barcode = barcode;
        this.unit = unit;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
        this.trackExpiry = trackExpiry;
        this.batches = batches;
        this.location = location;
        this.lastMovement = lastMovement;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public Boolean getTrackExpiry() { return trackExpiry; }
    public void setTrackExpiry(Boolean trackExpiry) { this.trackExpiry = trackExpiry; }

    public List<BatchInfo> getBatches() { return batches; }
    public void setBatches(List<BatchInfo> batches) { this.batches = batches; }

    public LocationInfo getLocation() { return location; }
    public void setLocation(LocationInfo location) { this.location = location; }

    public LastMovementInfo getLastMovement() { return lastMovement; }
    public void setLastMovement(LastMovementInfo lastMovement) { this.lastMovement = lastMovement; }

    // --- Nested inner classes ---

    public static class BatchInfo {
        private Long batchId;
        private String lotNumber;
        private LocalDate expiryDate;
        private Integer quantity;
        private Long daysUntilExpiry;

        public BatchInfo() {}

        public BatchInfo(Long batchId, String lotNumber, LocalDate expiryDate,
                         Integer quantity, Long daysUntilExpiry) {
            this.batchId = batchId;
            this.lotNumber = lotNumber;
            this.expiryDate = expiryDate;
            this.quantity = quantity;
            this.daysUntilExpiry = daysUntilExpiry;
        }

        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }

        public String getLotNumber() { return lotNumber; }
        public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

        public LocalDate getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Long getDaysUntilExpiry() { return daysUntilExpiry; }
        public void setDaysUntilExpiry(Long daysUntilExpiry) { this.daysUntilExpiry = daysUntilExpiry; }
    }

    public static class LocationInfo {
        private Long locationId;
        private String name;

        public LocationInfo() {}

        public LocationInfo(Long locationId, String name) {
            this.locationId = locationId;
            this.name = name;
        }

        public Long getLocationId() { return locationId; }
        public void setLocationId(Long locationId) { this.locationId = locationId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class LastMovementInfo {
        private MovementType type;
        private Integer quantity;
        private LocalDateTime createdAt;
        private String createdBy;

        public LastMovementInfo() {}

        public LastMovementInfo(MovementType type, Integer quantity,
                                 LocalDateTime createdAt, String createdBy) {
            this.type = type;
            this.quantity = quantity;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
        }

        public MovementType getType() { return type; }
        public void setType(MovementType type) { this.type = type; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    }
}
