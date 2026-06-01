package com.example.magazyn.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductResponse {

    private Long id;
    private String name;
    private String sku;
    private String description;
    private String unit;
    private Integer quantity;
    private BigDecimal price;
    private Integer minQuantity;
    private Long locationId;
    private LocalDateTime createdAt;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private LocalDate nearestExpiryDate;
    private Boolean trackExpiry;
    private String barcode;
    private Long categoryId;
    private Long defaultLocationId;

    public ProductResponse() {}

    public ProductResponse(Long id, String name, String sku, String barcode, String description, String unit,
                           Integer quantity, BigDecimal price, Integer minQuantity,
                           Long locationId, LocalDateTime createdAt) {
        this(id, name, sku, barcode, description, unit, quantity, price, minQuantity,
                locationId, createdAt, 0, quantity, null, false);
    }

    public ProductResponse(Long id, String name, String sku, String barcode, String description, String unit,
                           Integer quantity, BigDecimal price, Integer minQuantity,
                           Long locationId, LocalDateTime createdAt,
                           Integer reservedQuantity, Integer availableQuantity,
                           LocalDate nearestExpiryDate) {
        this(id, name, sku, barcode, description, unit, quantity, price, minQuantity,
                locationId, createdAt, reservedQuantity, availableQuantity, nearestExpiryDate, false);
    }

    public ProductResponse(Long id, String name, String sku, String barcode, String description, String unit,
                           Integer quantity, BigDecimal price, Integer minQuantity,
                           Long locationId, LocalDateTime createdAt,
                           Integer reservedQuantity, Integer availableQuantity,
                           LocalDate nearestExpiryDate, Boolean trackExpiry) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.barcode = barcode;
        this.description = description;
        this.unit = unit;
        this.quantity = quantity;
        this.price = price;
        this.minQuantity = minQuantity;
        this.locationId = locationId;
        this.createdAt = createdAt;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
        this.nearestExpiryDate = nearestExpiryDate;
        this.trackExpiry = trackExpiry;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public LocalDate getNearestExpiryDate() { return nearestExpiryDate; }
    public void setNearestExpiryDate(LocalDate nearestExpiryDate) { this.nearestExpiryDate = nearestExpiryDate; }

    public Boolean getTrackExpiry() { return trackExpiry; }
    public void setTrackExpiry(Boolean trackExpiry) { this.trackExpiry = trackExpiry; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getDefaultLocationId() { return defaultLocationId; }
    public void setDefaultLocationId(Long defaultLocationId) { this.defaultLocationId = defaultLocationId; }
}
