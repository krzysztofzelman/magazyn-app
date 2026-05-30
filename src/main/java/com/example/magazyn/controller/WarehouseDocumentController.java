package com.example.magazyn.controller;

import com.example.magazyn.dto.WarehouseDocumentRequest;
import com.example.magazyn.dto.WarehouseDocumentResponse;
import com.example.magazyn.entity.DocumentStatus;
import com.example.magazyn.entity.DocumentType;
import com.example.magazyn.service.WarehouseDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Warehouse Documents", description = "PZ/WZ warehouse document management")
@SecurityRequirement(name = "bearerAuth")
public class WarehouseDocumentController {

    private final WarehouseDocumentService documentService;

    public WarehouseDocumentController(WarehouseDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')")
    @Operation(summary = "Create a new warehouse document (PZ/WZ) with items")
    public ResponseEntity<WarehouseDocumentResponse> createDocument(
            @Valid @RequestBody WarehouseDocumentRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        WarehouseDocumentResponse created = documentService.createDocument(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get warehouse documents with filtering and pagination")
    public ResponseEntity<Page<WarehouseDocumentResponse>> getDocuments(
            @RequestParam(required = false) DocumentType type,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(documentService.getDocuments(type, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get warehouse document details with items")
    public ResponseEntity<WarehouseDocumentResponse> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')")
    @Operation(summary = "Confirm a warehouse document (PZ → stock receipt, WZ → stock issue)")
    public ResponseEntity<WarehouseDocumentResponse> confirmDocument(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(documentService.confirmDocument(id, username));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')")
    @Operation(summary = "Cancel a warehouse document (only DRAFT status)")
    public ResponseEntity<WarehouseDocumentResponse> cancelDocument(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(documentService.cancelDocument(id, username));
    }

    @GetMapping("/{id}/export/pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export warehouse document to PDF")
    public ResponseEntity<byte[]> exportDocumentPdf(@PathVariable Long id) {
        byte[] pdfBytes = documentService.exportDocumentPdf(id);
        WarehouseDocumentResponse doc = documentService.getDocumentById(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + doc.getNumber() + ".pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);
    }
}
