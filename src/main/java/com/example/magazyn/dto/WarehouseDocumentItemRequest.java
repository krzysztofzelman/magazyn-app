package com.example.magazyn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WarehouseDocumentItemRequest {

    @NotNull(message = "ID produktu jest wymagane")
    private Long productId;

    @NotNull(message = "Ilość jest wymagana")
    @Positive(message = "Ilość musi być dodatnia")
    private Integer quantity;

    @NotNull(message = "Cena jednostkowa jest wymagana")
    private BigDecimal unitPrice;

    private String lotNumber;

    private LocalDate expiryDate;

    private LocalDate manufacturingDate;

    public @NotNull(message = "ID produktu jest wymagane") Long getProductId() {
        return productId;
    }

    public void setProductId(@NotNull(message = "ID produktu jest wymagane") Long productId) {
        this.productId = productId;
    }

    public @NotNull(message = "Ilość jest wymagana") @Positive(message = "Ilość musi być dodatnia") Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(@NotNull(message = "Ilość jest wymagana") @Positive(message = "Ilość musi być dodatnia") Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public void setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
    }
}
