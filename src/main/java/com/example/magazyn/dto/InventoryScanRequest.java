package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class InventoryScanRequest {

    @NotBlank(message = "Kod kreskowy lokalizacji jest wymagany")
    private String locationBarcode;

    @NotBlank(message = "Kod kreskowy produktu jest wymagany")
    private String productBarcode;

    @NotNull(message = "Ilość jest wymagana")
    @Positive(message = "Ilość musi być dodatnia")
    private Double quantity;

    public String getLocationBarcode() { return locationBarcode; }
    public void setLocationBarcode(String locationBarcode) { this.locationBarcode = locationBarcode; }

    public String getProductBarcode() { return productBarcode; }
    public void setProductBarcode(String productBarcode) { this.productBarcode = productBarcode; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
}
