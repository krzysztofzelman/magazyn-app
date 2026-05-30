package com.example.magazyn.dto;

public class InventoryScanRequest {

    private String locationBarcode;
    private String productBarcode;
    private Double quantity;

    public String getLocationBarcode() { return locationBarcode; }
    public void setLocationBarcode(String locationBarcode) { this.locationBarcode = locationBarcode; }

    public String getProductBarcode() { return productBarcode; }
    public void setProductBarcode(String productBarcode) { this.productBarcode = productBarcode; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
}
