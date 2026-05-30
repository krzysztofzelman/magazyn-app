package com.example.magazyn.controller;

import com.example.magazyn.dto.LocationScanRequest;
import com.example.magazyn.dto.WzScanResponse;
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
@RequestMapping("/api/wz-documents")
@Tag(name = "Skaner WZ", description = "Obs\u0142uga skanowania lokalizacji dla dokument\u00f3w wydania (WZ)")
@SecurityRequirement(name = "bearerAuth")
public class WZScanController {

    private final WarehouseDocumentService documentService;

    public WZScanController(WarehouseDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/{docId}/items/{itemId}/scan-location")
    @Operation(summary = "Skanuj lokalizacj\u0119 dla pozycji WZ — weryfikuje czy produkt znajduje si\u0119 w podanej lokalizacji i zwraca dost\u0119pn\u0105 ilo\u015b\u0107")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WzScanResponse> scanLocationForItem(
            @PathVariable @Parameter(description = "ID dokumentu WZ") Long docId,
            @PathVariable @Parameter(description = "ID pozycji dokumentu") Long itemId,
            @Valid @RequestBody LocationScanRequest request,
            Authentication authentication) {
        WzScanResponse response = documentService.scanLocationForWzItem(
                docId, itemId, request.getBarcode(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
