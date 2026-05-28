package com.example.magazyn.dto;

import com.example.magazyn.entity.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class WarehouseDocumentRequest {

    @NotNull(message = "Typ dokumentu jest wymagany")
    private DocumentType type;

    @NotNull(message = "ID kontrahenta jest wymagane")
    private Long contractorId;

    private String notes;

    @NotEmpty(message = "Dokument musi zawierać co najmniej jedną pozycję")
    @Valid
    private List<WarehouseDocumentItemRequest> items;

    public @NotNull(message = "Typ dokumentu jest wymagany") DocumentType getType() {
        return type;
    }

    public void setType(@NotNull(message = "Typ dokumentu jest wymagany") DocumentType type) {
        this.type = type;
    }

    public @NotNull(message = "ID kontrahenta jest wymagane") Long getContractorId() {
        return contractorId;
    }

    public void setContractorId(@NotNull(message = "ID kontrahenta jest wymagane") Long contractorId) {
        this.contractorId = contractorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public @NotEmpty(message = "Dokument musi zawierać co najmniej jedną pozycję") @Valid List<WarehouseDocumentItemRequest> getItems() {
        return items;
    }

    public void setItems(@NotEmpty(message = "Dokument musi zawierać co najmniej jedną pozycję") @Valid List<WarehouseDocumentItemRequest> items) {
        this.items = items;
    }
}
