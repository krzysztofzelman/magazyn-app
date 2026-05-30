package com.example.magazyn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LocationStockResponse {

    private Long id;
    private Long locationId;
    private Long productId;
    private String productName;
    private String productSku;
    private String productUnit;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private LocalDateTime updatedAt;

    public LocationStockResponse() {}

    public LocationStockResponse(Long id, Long locationId, Long productId,
                                 String productName, String productSku,
                                 String productUnit, BigDecimal quantity,
                                 BigDecimal reservedQuantity, BigDecimal availableQuantity,
                                 LocalDateTime updatedAt) {
        this.id = id;
        this.locationId = locationId;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.productUnit = productUnit;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getProductUnit() { return productUnit; }
    public void setProductUnit(String productUnit) { this.productUnit = productUnit; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(BigDecimal reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal availableQuantity) { this.availableQuantity = availableQuantity; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
