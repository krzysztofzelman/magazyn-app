package com.example.magazyn.dto;

import com.example.magazyn.entity.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class StockMovementRequest {

    @NotNull(message = "Typ ruchu jest wymagany")
    private MovementType type;

    @NotNull(message = "Ilość jest wymagana")
    @Positive(message = "Ilość musi być większa od 0")
    private Integer quantity;

    private String note;

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
