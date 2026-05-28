package com.example.magazyn.dto;

public class ProductAvailabilityResponse {

    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;

    public ProductAvailabilityResponse() {}

    public ProductAvailabilityResponse(Long productId, String productName, String productSku,
                                       Integer quantity, Integer reservedQuantity,
                                       Integer availableQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
}
