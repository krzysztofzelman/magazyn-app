package com.example.magazyn.ksef.controller;

import com.example.magazyn.ksef.model.dto.KSeFAuditLogResponse;
import com.example.magazyn.ksef.model.dto.KSeFDashboardStats;
import com.example.magazyn.ksef.model.dto.KSeFSessionResponse;
import com.example.magazyn.ksef.service.KsefAuditService;
import com.example.magazyn.ksef.service.KsefAuthService;
import com.example.magazyn.ksef.service.KsefInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ksef/admin")
@Tag(name = "KSeF Admin", description = "KSeF administration — sessions, dashboard, audit")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class KsefAdminController {

    private final KsefAuthService authService;
    private final KsefInvoiceService invoiceService;
    private final KsefAuditService auditService;

    public KsefAdminController(KsefAuthService authService,
                               KsefInvoiceService invoiceService,
                               KsefAuditService auditService) {
        this.authService = authService;
        this.invoiceService = invoiceService;
        this.auditService = auditService;
    }

    // -- Sessions --

    @PostMapping("/session/init")
    @Operation(summary = "Initiate a new KSeF session")
    public ResponseEntity<KSeFSessionResponse> initSession(Authentication auth) {
        return ResponseEntity.ok(authService.getOrCreateSession(auth.getName()));
    }

    @PostMapping("/session/{sessionId}/close")
    @Operation(summary = "Close a KSeF session")
    public ResponseEntity<Map<String, String>> closeSession(
            @PathVariable Long sessionId, Authentication auth) {
        authService.closeSession(sessionId, auth.getName());
        return ResponseEntity.ok(Map.of("status", "closed"));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List all KSeF sessions")
    public ResponseEntity<List<KSeFSessionResponse>> getSessions() {
        return ResponseEntity.ok(authService.getSessions());
    }

    // -- Dashboard --

    @GetMapping("/dashboard")
    @Operation(summary = "Get KSeF dashboard statistics")
    public ResponseEntity<KSeFDashboardStats> getDashboard() {
        return ResponseEntity.ok(invoiceService.getDashboardStats());
    }

    // -- Audit --

    @GetMapping("/audit")
    @Operation(summary = "Get KSeF audit logs")
    public ResponseEntity<Page<KSeFAuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getAuditLogs(PageRequest.of(page, size)));
    }
}
