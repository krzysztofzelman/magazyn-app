package com.example.magazyn.controller;

import com.example.magazyn.dto.QuickIssueRequest;
import com.example.magazyn.dto.QuickReceiveRequest;
import com.example.magazyn.dto.ScannerResultResponse;
import com.example.magazyn.service.ScannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scanner")
@Tag(name = "Skaner kod\u00f3w kreskowych", description = "Szybkie operacje przez skaner kod\u00f3w kreskowych / QR")
@SecurityRequirement(name = "bearerAuth")
public class ScannerController {

    private final ScannerService scannerService;

    public ScannerController(ScannerService scannerService) {
        this.scannerService = scannerService;
    }

    @GetMapping("/lookup")
    @Operation(summary = "Szukaj produktu po SKU lub kodzie kreskowym")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScannerResultResponse> lookupByCode(
            @RequestParam @Parameter(description = "SKU lub kod kreskowy") String code,
            Authentication authentication) {
        ScannerResultResponse result = scannerService.lookupByCode(code, authentication.getName());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/quick-receive")
    @Operation(summary = "Szybkie przyj\u0119cie towaru przez skaner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScannerResultResponse> quickReceive(
            @RequestBody QuickReceiveRequest request,
            Authentication authentication) {
        ScannerResultResponse result = scannerService.quickReceive(
                request.getProductId(), request.getQuantity(), request.getLotNumber(),
                request.getExpiryDate(), request.getManufacturingDate(), request.getLocationId(),
                authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/quick-issue")
    @Operation(summary = "Szybkie wydanie towaru przez skaner")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAREHOUSE')")
    public ResponseEntity<ScannerResultResponse> quickIssue(
            @RequestBody QuickIssueRequest request,
            Authentication authentication) {
        ScannerResultResponse result = scannerService.quickIssue(
                request.getProductId(), request.getQuantity(), request.getBatchId(),
                request.getNote(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
