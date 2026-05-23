package com.example.magazyn.dto;

import com.example.magazyn.entity.MovementType;

public class StockMovementRequest {

    private MovementType type;
    private Integer quantity;
    private String note;

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
