package com.example.magazyn.controller;

import com.example.magazyn.entity.AuditLog;
import com.example.magazyn.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audyt", description = "Logi audytowe — dostęp tylko dla ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Pobierz logi audytowe", description = "Paginacja, filtry po username i action")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") @Parameter(description = "Numer strony") int page,
            @RequestParam(defaultValue = "50") @Parameter(description = "Rozmiar strony") int size,
            @RequestParam(required = false) @Parameter(description = "Filtr po nazwie u\u017cytkownika") String username,
            @RequestParam(required = false) @Parameter(description = "Filtr po akcji (np. CREATE_PRODUCT)") String action) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> result = auditLogService.getAuditLogs(username, action, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    @Operation(summary = "Eksportuj logi audytowe do CSV")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) @Parameter(description = "Filtr po nazwie u\u017cytkownika") String username,
            @RequestParam(required = false) @Parameter(description = "Filtr po akcji") String action) {

        Pageable all = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditLogService.getAuditLogs(username, action, all);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Username,Action,EntityType,EntityId,Details,IP,Timestamp\n");
        for (AuditLog log : logs.getContent()) {
            sb.append(log.getId()).append(",");
            sb.append(escapeCsv(log.getUsername())).append(",");
            sb.append(escapeCsv(log.getAction())).append(",");
            sb.append(escapeCsv(log.getEntityType())).append(",");
            sb.append(log.getEntityId() != null ? log.getEntityId() : "").append(",");
            sb.append(escapeCsv(log.getDetails())).append(",");
            sb.append(escapeCsv(log.getIpAddress())).append(",");
            sb.append(log.getTimestamp() != null ? log.getTimestamp().toString() : "").append("\n");
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String filename = "audit-" + dateStr + ".csv";
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(data);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
