package com.example.magazyn.dto;

import com.example.magazyn.entity.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class StockMovementRequest {

    @NotNull(message = "Typ ruchu jest wymagany")
    private MovementType type;

    @NotNull(message = "Ilość jest wymagana")
    @PositiveOrZero(message = "Ilość musi być większa lub równa 0")
    private Integer quantity;

    private String note;

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
