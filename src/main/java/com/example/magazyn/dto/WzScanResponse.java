package com.example.magazyn.dto;

import java.math.BigDecimal;

public class WzScanResponse {

    private Long itemId;
    private Long productId;
    private String productName;
    private String productSku;
    private String productUnit;
    private Integer requestedQuantity;
    private Long locationId;
    private String locationCode;
    private BigDecimal availableQuantity;
    private boolean sufficientStock;

    public WzScanResponse() {}

    public WzScanResponse(Long itemId, Long productId, String productName,
                          String productSku, String productUnit,
                          Integer requestedQuantity, Long locationId,
                          String locationCode, BigDecimal availableQuantity,
                          boolean sufficientStock) {
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.productUnit = productUnit;
        this.requestedQuantity = requestedQuantity;
        this.locationId = locationId;
        this.locationCode = locationCode;
        this.availableQuantity = availableQuantity;
        this.sufficientStock = sufficientStock;
    }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getProductUnit() { return productUnit; }
    public void setProductUnit(String productUnit) { this.productUnit = productUnit; }

    public Integer getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(Integer requestedQuantity) { this.requestedQuantity = requestedQuantity; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal availableQuantity) { this.availableQuantity = availableQuantity; }

    public boolean isSufficientStock() { return sufficientStock; }
    public void setSufficientStock(boolean sufficientStock) { this.sufficientStock = sufficientStock; }
}
