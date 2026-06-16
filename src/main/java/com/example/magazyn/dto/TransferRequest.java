package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TransferRequest {

    @NotBlank(message = "Kod lokalizacji źródłowej jest wymagany")
    private String fromBarcode;

    @NotBlank(message = "Kod lokalizacji docelowej jest wymagany")
    private String toBarcode;

    @NotNull(message = "ID produktu jest wymagane")
    private Long productId;

    @NotNull(message = "Ilość jest wymagana")
    @Positive(message = "Ilość musi być dodatnia")
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
