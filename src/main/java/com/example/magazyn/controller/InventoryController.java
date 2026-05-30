package com.example.magazyn.controller;

import com.example.magazyn.dto.InventoryItemResponse;
import com.example.magazyn.dto.InventoryReportResponse;
import com.example.magazyn.dto.InventoryScanRequest;
import com.example.magazyn.dto.InventorySessionRequest;
import com.example.magazyn.dto.InventorySessionResponse;
import com.example.magazyn.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inwentaryzacja", description = "Zarz\u0105dzanie sesjami inwentaryzacji")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/sessions")
    @Operation(summary = "Pobierz wszystkie sesje inwentaryzacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InventorySessionResponse>> getAllSessions() {
        return ResponseEntity.ok(inventoryService.getAllSessions());
    }

    @PostMapping("/sessions")
    @Operation(summary = "Utw\u00f3rz now\u0105 sesj\u0119 inwentaryzacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InventorySessionResponse> createSession(
            @Valid @RequestBody InventorySessionRequest request,
            Authentication authentication) {
        InventorySessionResponse response = inventoryService.createSession(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Pobierz szczeg\u00f3\u0142y sesji inwentaryzacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InventorySessionResponse> getSession(@PathVariable Long id) {
        InventorySessionResponse response = inventoryService.getSession(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{id}/scan")
    @Operation(summary = "Zarejestruj zeskanowan\u0105 ilo\u015b\u0107 produktu w danej lokalizacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InventoryItemResponse> scan(
            @PathVariable Long id,
            @Valid @RequestBody InventoryScanRequest request,
            Authentication authentication) {
        InventoryItemResponse response = inventoryService.scan(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sessions/{id}/report")
    @Operation(summary = "Pobierz raport inwentaryzacji (wszystkie pozycje z r\u00f3\u017cnicami)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InventoryReportResponse> getReport(@PathVariable Long id) {
        InventoryReportResponse response = inventoryService.getReport(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{id}/close")
    @Operation(summary = "Zamknij sesj\u0119 inwentaryzacji i zaktualizuj stany magazynowe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InventorySessionResponse> closeSession(
            @PathVariable Long id,
            Authentication authentication) {
        InventorySessionResponse response = inventoryService.closeSession(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
