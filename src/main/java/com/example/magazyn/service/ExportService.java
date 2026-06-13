package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.util.CsvUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class ExportService {

    private final ProductRepository productRepository;

    public ExportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public byte[] exportProductsCsv(Set<String> fields) {
        List<Product> products = productRepository.findByTenantId(TenantContext.getTenantId());
        List<String> headers = resolveProductHeaders(fields);
        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append(String.join(",", headers));
        sb.append("\n");

        // Data rows
        for (Product p : products) {
            sb.append(csvRow(p, headers));
            sb.append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportProductsExcel(Set<String> fields) {
        List<Product> products = productRepository.findByTenantId(TenantContext.getTenantId());
        List<String> headers = resolveProductHeaders(fields);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowIdx++);
                List<String> values = csvValues(p, headers);
                for (int i = 0; i < values.size(); i++) {
                    Cell cell = row.createCell(i);
                    String val = values.get(i);
                    // Try numeric
                    try {
                        cell.setCellValue(Double.parseDouble(val.replace(",", ".")));
                    } catch (NumberFormatException e) {
                        cell.setCellValue(val);
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    public byte[] exportStockCsv(Set<String> fields) {
        List<Product> products = productRepository.findByTenantId(TenantContext.getTenantId());
        List<String> headers = resolveStockHeaders(fields);
        StringBuilder sb = new StringBuilder();

        sb.append(String.join(",", headers));
        sb.append("\n");

        for (Product p : products) {
            sb.append(stockCsvRow(p, headers));
            sb.append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportStockExcel(Set<String> fields) {
        List<Product> products = productRepository.findByTenantId(TenantContext.getTenantId());
        List<String> headers = resolveStockHeaders(fields);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Stock");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowIdx++);
                List<String> values = stockValues(p, headers);
                for (int i = 0; i < values.size(); i++) {
                    Cell cell = row.createCell(i);
                    String val = values.get(i);
                    try {
                        cell.setCellValue(Double.parseDouble(val.replace(",", ".")));
                    } catch (NumberFormatException e) {
                        cell.setCellValue(val);
                    }
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    // --- Product helpers ---

    private static final List<String> ALL_PRODUCT_FIELDS = List.of(
            "id", "name", "sku", "description", "unit", "quantity", "price", "minQuantity", "createdAt"
    );

    private static final List<String> PRODUCT_LABELS = List.of(
            "ID", "Nazwa", "SKU", "Opis", "Jednostka", "Ilość", "Cena", "Min. stan", "Data utworzenia"
    );

    private List<String> resolveProductHeaders(Set<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return ALL_PRODUCT_FIELDS;
        }
        return ALL_PRODUCT_FIELDS.stream()
                .filter(fields::contains)
                .toList();
    }

    private String csvRow(Product p, List<String> headers) {
        return String.join(",", csvValues(p, headers));
    }

    private List<String> csvValues(Product p, List<String> headers) {
        return headers.stream()
                .map(f -> switch (f) {
                    case "id" -> String.valueOf(p.getId());
                    case "name" -> CsvUtils.escapeCsv(p.getName());
                    case "sku" -> CsvUtils.escapeCsv(p.getSku());
                    case "description" -> CsvUtils.escapeCsv(p.getDescription() != null ? p.getDescription() : "");
                    case "unit" -> CsvUtils.escapeCsv(p.getUnit());
                    case "quantity" -> String.valueOf(p.getQuantity());
                    case "price" -> p.getPrice() != null ? p.getPrice().toString() : "0";
                    case "minQuantity" -> String.valueOf(p.getMinQuantity());
                    case "createdAt" -> p.getCreatedAt() != null ? p.getCreatedAt().toString() : "";
                    default -> "";
                })
                .toList();
    }

    // --- Stock helpers ---

    private static final List<String> ALL_STOCK_FIELDS = List.of(
            "productId", "productName", "sku", "unit", "quantity", "price", "stockValue", "minQuantity"
    );

    private static final List<String> STOCK_LABELS = List.of(
            "ID produktu", "Nazwa produktu", "SKU", "Jednostka", "Stan magazynowy", "Cena", "Wartość stanu", "Min. stan"
    );

    private List<String> resolveStockHeaders(Set<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return ALL_STOCK_FIELDS;
        }
        return ALL_STOCK_FIELDS.stream()
                .filter(fields::contains)
                .toList();
    }

    private String stockCsvRow(Product p, List<String> headers) {
        return String.join(",", stockValues(p, headers));
    }

    private List<String> stockValues(Product p, List<String> headers) {
        BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
        BigDecimal stockValue = price.multiply(BigDecimal.valueOf(p.getQuantity()));

        return headers.stream()
                .map(f -> switch (f) {
                    case "productId" -> String.valueOf(p.getId());
                    case "productName" -> CsvUtils.escapeCsv(p.getName());
                    case "sku" -> CsvUtils.escapeCsv(p.getSku());
                    case "unit" -> CsvUtils.escapeCsv(p.getUnit());
                    case "quantity" -> String.valueOf(p.getQuantity());
                    case "price" -> price.toString();
                    case "stockValue" -> stockValue.toString();
                    case "minQuantity" -> String.valueOf(p.getMinQuantity());
                    default -> "";
                })
                .toList();
    }
}
