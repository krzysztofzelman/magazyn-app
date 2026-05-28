package com.example.magazyn.controller;

import com.example.magazyn.dto.BatchResponse;
import com.example.magazyn.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@Tag(name = "Partie", description = "Zarządzanie partiami towaru z datami ważności")
@SecurityRequirement(name = "bearerAuth")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping("/expiring")
    @Operation(summary = "Pobierz partie wygasające", description = "Zwraca partie, których data ważności upływa w ciągu N dni")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BatchResponse>> getExpiringBatches(
            @RequestParam(defaultValue = "30") @Parameter(description = "Liczba dni") int days) {
        return ResponseEntity.ok(batchService.getExpiringBatches(days));
    }

    @GetMapping("/expired")
    @Operation(summary = "Pobierz partie przeterminowane", description = "Zwraca partie po dacie ważności")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BatchResponse>> getExpiredBatches() {
        return ResponseEntity.ok(batchService.getExpiredBatches());
    }
}
