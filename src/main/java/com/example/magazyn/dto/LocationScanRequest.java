package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;

public class LocationScanRequest {

    @NotBlank(message = "Kod kreskowy lokalizacji jest wymagany")
    private String barcode;

    public @NotBlank(message = "Kod kreskowy lokalizacji jest wymagany") String getBarcode() {
        return barcode;
    }

    public void setBarcode(@NotBlank(message = "Kod kreskowy lokalizacji jest wymagany") String barcode) {
        this.barcode = barcode;
    }
}
