package com.example.magazyn.dto;

public class QuickIssueRequest {
    private Long productId;
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
