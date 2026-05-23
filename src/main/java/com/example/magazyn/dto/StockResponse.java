package com.example.magazyn.dto;

public class StockResponse {

    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;

    public StockResponse() {}

    public StockResponse(Long productId, String productName, String sku, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.quantity = quantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
