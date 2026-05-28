package com.example.magazyn.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WarehouseDocumentItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productUnit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String lotNumber;
    private LocalDate expiryDate;
    private LocalDate manufacturingDate;

    public WarehouseDocumentItemResponse() {}

    public WarehouseDocumentItemResponse(Long id, Long productId, String productName, String productSku,
                                         String productUnit, Integer quantity, BigDecimal unitPrice,
                                         BigDecimal totalPrice) {
        this(id, productId, productName, productSku, productUnit, quantity, unitPrice, totalPrice, null, null, null);
    }

    public WarehouseDocumentItemResponse(Long id, Long productId, String productName, String productSku,
                                         String productUnit, Integer quantity, BigDecimal unitPrice,
                                         BigDecimal totalPrice, String lotNumber,
                                         LocalDate expiryDate, LocalDate manufacturingDate) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.productUnit = productUnit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.lotNumber = lotNumber;
        this.expiryDate = expiryDate;
        this.manufacturingDate = manufacturingDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    public String getProductUnit() { return productUnit; }
    public void setProductUnit(String productUnit) { this.productUnit = productUnit; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }
}
