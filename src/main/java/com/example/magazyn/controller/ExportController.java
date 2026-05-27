package com.example.magazyn.controller;

import com.example.magazyn.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Tag(name = "Eksport", description = "Eksport produkt\u00f3w i stan\u00f3w magazynowych do CSV/XLSX")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/products/export/csv")
    @Operation(summary = "Eksportuj produkty do CSV lub XLSX")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(defaultValue = "csv") @Parameter(description = "Format: csv lub xlsx") String format,
            @RequestParam(required = false) @Parameter(description = "Lista p\u00f3l do eksportu") Set<String> fields) {

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        byte[] data;
        String filename;
        MediaType contentType;

        if ("xlsx".equalsIgnoreCase(format)) {
            data = exportService.exportProductsExcel(fields);
            filename = "products-" + dateStr + ".xlsx";
            contentType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            data = exportService.exportProductsCsv(fields);
            filename = "products-" + dateStr + ".csv";
            contentType = MediaType.parseMediaType("text/csv");
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(data);
    }

    @GetMapping("/stock/export/excel")
    @Operation(summary = "Eksportuj stan magazynowy do CSV lub XLSX")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportStock(
            @RequestParam(defaultValue = "xlsx") @Parameter(description = "Format: csv lub xlsx") String format,
            @RequestParam(required = false) @Parameter(description = "Lista p\u00f3l do eksportu") Set<String> fields) {

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        byte[] data;
        String filename;
        MediaType contentType;

        if ("csv".equalsIgnoreCase(format)) {
            data = exportService.exportStockCsv(fields);
            filename = "stock-" + dateStr + ".csv";
            contentType = MediaType.parseMediaType("text/csv");
        } else {
            data = exportService.exportStockExcel(fields);
            filename = "stock-" + dateStr + ".xlsx";
            contentType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(data);
    }
}
