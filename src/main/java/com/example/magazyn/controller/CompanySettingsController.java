package com.example.magazyn.controller;

import com.example.magazyn.dto.CompanySettingsRequest;
import com.example.magazyn.dto.CompanySettingsResponse;
import com.example.magazyn.service.CompanySettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company-settings")
@Tag(name = "Company Settings", description = "Seller company information for invoices")
@SecurityRequirement(name = "bearerAuth")
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    public CompanySettingsController(CompanySettingsService companySettingsService) {
        this.companySettingsService = companySettingsService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current company settings")
    public ResponseEntity<CompanySettingsResponse> getSettings() {
        CompanySettingsResponse settings = companySettingsService.getSettings();
        if (settings == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create or update company settings")
    public ResponseEntity<CompanySettingsResponse> upsertSettings(
            @Valid @RequestBody CompanySettingsRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(companySettingsService.upsertSettings(request, username));
    }
}
