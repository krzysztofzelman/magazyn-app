package com.example.magazyn.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BatchResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String lotNumber;
    private LocalDate expiryDate;
    private LocalDate manufacturingDate;
    private Integer quantity;
    private Long locationId;
    private LocalDateTime createdAt;

    public BatchResponse() {}

    public BatchResponse(Long id, Long productId, String productName, String productSku,
                         String lotNumber, LocalDate expiryDate, LocalDate manufacturingDate,
                         Integer quantity, Long locationId, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.lotNumber = lotNumber;
        this.expiryDate = expiryDate;
        this.manufacturingDate = manufacturingDate;
        this.quantity = quantity;
        this.locationId = locationId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
