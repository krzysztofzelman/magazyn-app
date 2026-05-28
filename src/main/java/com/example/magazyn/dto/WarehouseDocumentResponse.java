package com.example.magazyn.dto;

import com.example.magazyn.entity.DocumentStatus;
import com.example.magazyn.entity.DocumentType;

import java.time.LocalDateTime;
import java.util.List;

public class WarehouseDocumentResponse {

    private Long id;
    private String number;
    private DocumentType type;
    private DocumentStatus status;
    private Long contractorId;
    private String contractorName;
    private String contractorTaxId;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private String createdBy;
    private String notes;
    private List<WarehouseDocumentItemResponse> items;

    public WarehouseDocumentResponse() {}

    public WarehouseDocumentResponse(Long id, String number, DocumentType type, DocumentStatus status,
                                     Long contractorId, String contractorName, String contractorTaxId,
                                     LocalDateTime createdAt, LocalDateTime confirmedAt, String createdBy,
                                     String notes, List<WarehouseDocumentItemResponse> items) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.status = status;
        this.contractorId = contractorId;
        this.contractorName = contractorName;
        this.contractorTaxId = contractorTaxId;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.createdBy = createdBy;
        this.notes = notes;
        this.items = items;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public Long getContractorId() { return contractorId; }
    public void setContractorId(Long contractorId) { this.contractorId = contractorId; }
    public String getContractorName() { return contractorName; }
    public void setContractorName(String contractorName) { this.contractorName = contractorName; }
    public String getContractorTaxId() { return contractorTaxId; }
    public void setContractorTaxId(String contractorTaxId) { this.contractorTaxId = contractorTaxId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<WarehouseDocumentItemResponse> getItems() { return items; }
    public void setItems(List<WarehouseDocumentItemResponse> items) { this.items = items; }
}
