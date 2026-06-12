package com.example.magazyn.controller;

import com.example.magazyn.dto.ApiKeyResponse;
import com.example.magazyn.dto.RegisterTenantRequest;
import com.example.magazyn.dto.TenantRegistrationResponse;
import com.example.magazyn.dto.TenantResponse;
import com.example.magazyn.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenanty", description = "Zarz\u0105dzanie tenantami")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/register")
    @Operation(summary = "Rejestracja nowej firmy (tenanta)", description = "Tworzy nowego tenanta z kontem administratora")
    public ResponseEntity<TenantRegistrationResponse> register(@Valid @RequestBody RegisterTenantRequest request) {
        TenantRegistrationResponse response = tenantService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Informacje o bie\u017C\u0105cym tenancie")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantResponse> getCurrentTenant() {
        TenantResponse response = tenantService.getCurrentTenantInfo();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api-key")
    @Operation(summary = "Pobierz klucz API dla bie\u017C\u0105cego tenanta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiKeyResponse> getApiKey() {
        return ResponseEntity.ok(tenantService.getApiKey());
    }

    @PostMapping("/api-key/regenerate")
    @Operation(summary = "Wygeneruj nowy klucz API")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiKeyResponse> regenerateApiKey() {
        return ResponseEntity.ok(tenantService.regenerateApiKey());
    }
}
