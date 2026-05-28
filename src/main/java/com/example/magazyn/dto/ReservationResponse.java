package com.example.magazyn.dto;

import com.example.magazyn.entity.ReservationReferenceType;
import com.example.magazyn.entity.ReservationStatus;

import java.time.LocalDateTime;

public class ReservationResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private ReservationReferenceType referenceType;
    private String referenceId;
    private ReservationStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String notes;

    public ReservationResponse() {}

    public ReservationResponse(Long id, Long productId, String productName, String productSku,
                               Integer quantity, ReservationReferenceType referenceType,
                               String referenceId, ReservationStatus status,
                               String createdBy, LocalDateTime createdAt,
                               LocalDateTime expiresAt, String notes) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public ReservationReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(ReservationReferenceType referenceType) { this.referenceType = referenceType; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
