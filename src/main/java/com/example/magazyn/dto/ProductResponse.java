package com.example.magazyn.dto;

import java.math.BigDecimal;
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

    public ProductResponse() {}

    public ProductResponse(Long id, String name, String sku, String description, String unit,
                           Integer quantity, BigDecimal price, Integer minQuantity,
                           Long locationId, LocalDateTime createdAt) {
        this(id, name, sku, description, unit, quantity, price, minQuantity,
                locationId, createdAt, 0, quantity);
    }

    public ProductResponse(Long id, String name, String sku, String description, String unit,
                           Integer quantity, BigDecimal price, Integer minQuantity,
                           Long locationId, LocalDateTime createdAt,
                           Integer reservedQuantity, Integer availableQuantity) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.unit = unit;
        this.quantity = quantity;
        this.price = price;
        this.minQuantity = minQuantity;
        this.locationId = locationId;
        this.createdAt = createdAt;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
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
}
