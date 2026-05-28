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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Confirm a warehouse document (PZ → stock receipt, WZ → stock issue)")
    public ResponseEntity<WarehouseDocumentResponse> confirmDocument(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(documentService.confirmDocument(id, username));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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
        WarehouseDocumentResponse doc = documentService.getDocumentById(id);
        byte[] pdfBytes = generatePdf(doc);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + doc.getNumber() + ".pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);
    }

    private byte[] generatePdf(WarehouseDocumentResponse doc) {
        try {
            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                 org.apache.pdfbox.pdmodel.PDDocument pdfDoc = new org.apache.pdfbox.pdmodel.PDDocument()) {

                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(
                        org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
                pdfDoc.addPage(page);

                try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                             new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDoc, page)) {
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16);
                    cs.beginText();
                    cs.newLineAtOffset(50, 750);
                    cs.showText("Dokument " + doc.getType() + " nr " + doc.getNumber());
                    cs.endText();

                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    cs.beginText();
                    cs.newLineAtOffset(50, 720);
                    cs.showText("Status: " + doc.getStatus());
                    cs.endText();
                    cs.beginText();
                    cs.newLineAtOffset(50, 705);
                    cs.showText("Kontrahent: " + doc.getContractorName());
                    cs.endText();
                    cs.beginText();
                    cs.newLineAtOffset(50, 690);
                    cs.showText("NIP: " + (doc.getContractorTaxId() != null ? doc.getContractorTaxId() : "-"));
                    cs.endText();
                    cs.beginText();
                    cs.newLineAtOffset(50, 675);
                    cs.showText("Data utworzenia: " + doc.getCreatedAt());
                    cs.endText();
                    cs.beginText();
                    cs.newLineAtOffset(50, 660);
                    cs.showText("Utworzyl: " + doc.getCreatedBy());
                    cs.endText();
                    if (doc.getNotes() != null && !doc.getNotes().isBlank()) {
                        cs.beginText();
                        cs.newLineAtOffset(50, 645);
                        cs.showText("Uwagi: " + doc.getNotes());
                        cs.endText();
                    }

                    // Table header
                    float y = 620;
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10);
                    drawTableCell(cs, 50, y, 40, "Lp.");
                    drawTableCell(cs, 90, y, 200, "Produkt");
                    drawTableCell(cs, 290, y, 80, "Ilość");
                    drawTableCell(cs, 370, y, 80, "Cena");
                    drawTableCell(cs, 450, y, 90, "Wartość");

                    y -= 20;
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);

                    int lp = 1;
                    for (var item : doc.getItems()) {
                        if (y < 50) break;
                        drawTableCell(cs, 50, y, 40, String.valueOf(lp++));
                        drawTableCell(cs, 90, y, 200, item.getProductName());
                        drawTableCell(cs, 290, y, 80, item.getQuantity() + " " + item.getProductUnit());
                        drawTableCell(cs, 370, y, 80, item.getUnitPrice().toString());
                        drawTableCell(cs, 450, y, 90, item.getTotalPrice().toString());
                        y -= 20;
                    }

                    if (doc.getConfirmedAt() != null) {
                        y -= 20;
                        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10);
                        cs.beginText();
                        cs.newLineAtOffset(50, y);
                        cs.showText("Zatwierdzono: " + doc.getConfirmedAt());
                        cs.endText();
                    }
                }

                pdfDoc.save(baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void drawTableCell(org.apache.pdfbox.pdmodel.PDPageContentStream cs,
                               float x, float y, float width, String text) throws java.io.IOException {
        cs.beginText();
        cs.newLineAtOffset(x + 2, y);
        cs.showText(text);
        cs.endText();
    }
}
