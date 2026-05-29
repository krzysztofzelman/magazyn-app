package com.example.magazyn.controller;

import com.example.magazyn.dto.ScannerResultResponse;
import com.example.magazyn.service.ScannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

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
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> lookupByCode(
            @RequestParam @Parameter(description = "SKU lub kod kreskowy") String code) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            ScannerResultResponse result = scannerService.lookupByCode(code, username);
            return ResponseEntity.ok(result);
        } catch (com.example.magazyn.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "NOT_FOUND", "scannedCode", code, "message", e.getMessage()));
        }
    }

    @PostMapping("/quick-receive")
    @Operation(summary = "Szybkie przyj\u0119cie towaru przez skaner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScannerResultResponse> quickReceive(
            @RequestBody @Parameter(description = "Dane przyj\u0119cia") QuickReceiveRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ScannerResultResponse result = scannerService.quickReceive(
                request.productId, request.quantity, request.lotNumber,
                request.expiryDate, request.manufacturingDate, request.locationId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/quick-issue")
    @Operation(summary = "Szybkie wydanie towaru przez skaner")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ScannerResultResponse> quickIssue(
            @RequestBody @Parameter(description = "Dane wydania") QuickIssueRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ScannerResultResponse result = scannerService.quickIssue(
                request.productId, request.quantity, request.batchId, request.note, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // --- Request DTOs (package-private, nested in controller for simplicity) ---

    public static class QuickReceiveRequest {
        public Long productId;
        public Integer quantity;
        public String lotNumber;
        public LocalDate expiryDate;
        public LocalDate manufacturingDate;
        public Long locationId;
    }

    public static class QuickIssueRequest {
        public Long productId;
        public Integer quantity;
        public Long batchId;
        public String note;
    }
}
