package com.example.magazyn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class QuickIssueRequest {
    @NotNull(message = "ID produktu jest wymagane")
    private Long productId;

    @NotNull(message = "Ilość jest wymagana")
    @Positive(message = "Ilość musi być dodatnia")
    private Integer quantity;

    private Long batchId;
    private String note;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
