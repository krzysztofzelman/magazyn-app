package com.example.magazyn.controller;

import com.example.magazyn.dto.PendingScanRequest;
import com.example.magazyn.dto.PendingScanResponse;
import com.example.magazyn.service.PendingScanService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pending-scans")
@Tag(name = "Koszyk skanów", description = "Tymczasowy koszyk zeskanowanych kodów przed zatwierdzeniem")
@SecurityRequirement(name = "bearerAuth")
public class PendingScanController {

    private static final Logger log = LoggerFactory.getLogger(PendingScanController.class);

    private final PendingScanService pendingScanService;

    public PendingScanController(PendingScanService pendingScanService) {
        this.pendingScanService = pendingScanService;
    }

    @PostMapping
    @Operation(summary = "Dodaj zeskanowany kod do koszyka")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PendingScanResponse> addScan(
            @Valid @RequestBody PendingScanRequest request,
            Authentication authentication) {
        log.info("POST /api/pending-scans mode={}, barcode={}", request.getMode(), request.getBarcode());
        PendingScanResponse result = pendingScanService.addScan(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @Operation(summary = "Pobierz listę oczekujących skanów dla trybu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PendingScanResponse>> listScans(
            @RequestParam @Parameter(description = "Tryb skanowania: PZ, WZ, TRANSFER, INVENTORY") String mode,
            Authentication authentication) {
        log.debug("GET /api/pending-scans?mode={}", mode);
        List<PendingScanResponse> scans = pendingScanService.listScans(mode, authentication.getName());
        return ResponseEntity.ok(scans);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usuń pojedynczy skan z koszyka")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeScan(
            @PathVariable @Parameter(description = "ID skanu") Long id,
            Authentication authentication) {
        log.info("DELETE /api/pending-scans/{}", id);
        pendingScanService.removeScan(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Wyczyść wszystkie oczekujące skany dla trybu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> clearScans(
            @RequestParam @Parameter(description = "Tryb skanowania") String mode,
            Authentication authentication) {
        log.info("DELETE /api/pending-scans?mode={}", mode);
        int removed = pendingScanService.clearScans(mode, authentication.getName());
        return ResponseEntity.ok(Map.of("removed", removed));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Zatwierdź wszystkie oczekujące skany dla trybu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> confirmScans(
            @RequestParam @Parameter(description = "Tryb skanowania") String mode,
            Authentication authentication) {
        log.info("POST /api/pending-scans/confirm?mode={} by user={}", mode, authentication.getName());
        int confirmed = pendingScanService.confirmScans(mode, authentication.getName());
        return ResponseEntity.ok(Map.of("confirmed", confirmed));
    }
}
