package com.example.magazyn.dto;

public class TransferRequest {

    private String fromBarcode;
    private String toBarcode;
    private Long productId;
    private Double quantity;

    public String getFromBarcode() { return fromBarcode; }
    public void setFromBarcode(String fromBarcode) { this.fromBarcode = fromBarcode; }

    public String getToBarcode() { return toBarcode; }
    public void setToBarcode(String toBarcode) { this.toBarcode = toBarcode; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
}
