package com.example.magazyn.controller;

import com.example.magazyn.service.ExportService;
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
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/products/export/csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) Set<String> fields) {

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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportStock(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) Set<String> fields) {

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
