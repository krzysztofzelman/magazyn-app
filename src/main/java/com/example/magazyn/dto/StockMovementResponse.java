package com.example.magazyn.dto;

import com.example.magazyn.entity.MovementType;

import java.time.LocalDateTime;

public class StockMovementResponse {

    private Long id;
    private Long productId;
    private String productName;
    private MovementType type;
    private Integer quantity;
    private String note;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long batchId;

    public StockMovementResponse() {}

    public StockMovementResponse(Long id, Long productId, String productName, MovementType type,
                                 Integer quantity, String note, LocalDateTime createdAt, String createdBy) {
        this(id, productId, productName, type, quantity, note, createdAt, createdBy, null);
    }

    public StockMovementResponse(Long id, Long productId, String productName, MovementType type,
                                 Integer quantity, String note, LocalDateTime createdAt, String createdBy,
                                 Long batchId) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.note = note;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.batchId = batchId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
}
