package com.example.magazyn.controller;

import com.example.magazyn.dto.LocationResponse;
import com.example.magazyn.dto.LocationScanRequest;
import com.example.magazyn.dto.WarehouseDocumentItemResponse;
import com.example.magazyn.service.WarehouseDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pz-documents")
@Tag(name = "Skaner PZ", description = "Obs\u0142uga skanowania lokalizacji dla dokument\u00f3w przyj\u0119cia (PZ)")
@SecurityRequirement(name = "bearerAuth")
public class PZScanController {

    private final WarehouseDocumentService documentService;

    public PZScanController(WarehouseDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/{docId}/scan-location")
    @Operation(summary = "Przypisz lokalizacj\u0119 do dokumentu PZ (do wszystkich pozycji bez lokalizacji)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LocationResponse> scanLocationForDocument(
            @PathVariable @Parameter(description = "ID dokumentu PZ") Long docId,
            @Valid @RequestBody LocationScanRequest request) {
        LocationResponse response = documentService.scanLocationForDocument(docId, request.getBarcode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{docId}/items/{itemId}/scan-location")
    @Operation(summary = "Przypisz lokalizacj\u0119 do konkretnej pozycji PZ")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WarehouseDocumentItemResponse> scanLocationForItem(
            @PathVariable @Parameter(description = "ID dokumentu PZ") Long docId,
            @PathVariable @Parameter(description = "ID pozycji dokumentu") Long itemId,
            @Valid @RequestBody LocationScanRequest request,
            Authentication authentication) {
        WarehouseDocumentItemResponse response = documentService.scanLocationForItem(
                docId, itemId, request.getBarcode(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
