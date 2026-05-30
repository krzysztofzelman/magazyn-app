package com.example.magazyn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryItemResponse {

    private Long id;
    private Long sessionId;
    private Long locationId;
    private String locationCode;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal expectedQuantity;
    private BigDecimal countedQuantity;
    private BigDecimal difference;
    private LocalDateTime scannedAt;
    private String scannedBy;

    public InventoryItemResponse() {}

    public InventoryItemResponse(Long id, Long sessionId, Long locationId, String locationCode,
                                 Long productId, String productName, String productSku,
                                 BigDecimal expectedQuantity, BigDecimal countedQuantity,
                                 BigDecimal difference, LocalDateTime scannedAt, String scannedBy) {
        this.id = id;
        this.sessionId = sessionId;
        this.locationId = locationId;
        this.locationCode = locationCode;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.expectedQuantity = expectedQuantity;
        this.countedQuantity = countedQuantity;
        this.difference = difference;
        this.scannedAt = scannedAt;
        this.scannedBy = scannedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public BigDecimal getExpectedQuantity() { return expectedQuantity; }
    public void setExpectedQuantity(BigDecimal expectedQuantity) { this.expectedQuantity = expectedQuantity; }

    public BigDecimal getCountedQuantity() { return countedQuantity; }
    public void setCountedQuantity(BigDecimal countedQuantity) { this.countedQuantity = countedQuantity; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }

    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }

    public String getScannedBy() { return scannedBy; }
    public void setScannedBy(String scannedBy) { this.scannedBy = scannedBy; }
}
