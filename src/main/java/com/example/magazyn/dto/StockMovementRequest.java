package com.example.magazyn.dto;

import com.example.magazyn.entity.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public class StockMovementRequest {

    @NotNull(message = "Typ ruchu jest wymagany")
    private MovementType type;

    @NotNull(message = "Ilość jest wymagana")
    @PositiveOrZero(message = "Ilość musi być większa lub równa 0")
    private Integer quantity;

    private String note;

    private Long batchId;

    private String lotNumber;

    private LocalDate expiryDate;

    private LocalDate manufacturingDate;

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }
}
