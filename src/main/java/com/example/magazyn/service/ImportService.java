package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.ImportResult;
import com.example.magazyn.dto.ImportResult.RowError;
import com.example.magazyn.entity.Product;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "name", "sku", "quantity", "min_quantity", "price"
    );

    private final ProductRepository productRepository;

    public ImportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ImportResult importProducts(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new InvalidOperationException("Nazwa pliku jest wymagana");
        }

        String lower = filename.toLowerCase();
        List<String[]> rows;

        try {
            if (lower.endsWith(".xlsx")) {
                rows = parseXlsx(file);
            } else if (lower.endsWith(".csv")) {
                rows = parseCsv(file);
            } else {
                throw new InvalidOperationException("Nieobs\u0142ugiwany format pliku. U\u017Cyj .csv lub .xlsx");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidOperationException("Nie uda\u0142o si\u0119 odczyta\u0107 pliku: " + e.getMessage());
        }

        if (rows.size() < 2) {
            throw new InvalidOperationException("Plik jest pusty lub zawiera tylko nag\u0142\u00f3wek");
        }

        // Parse header
        String[] header = rows.get(0);
        Map<String, Integer> columnIndex = buildColumnIndex(header);

        // Validate required columns
        List<String> missing = new ArrayList<>();
        for (String col : REQUIRED_COLUMNS) {
            if (!columnIndex.containsKey(col)) {
                missing.add(col);
            }
        }
        if (!missing.isEmpty()) {
            throw new InvalidOperationException(
                    "Brakuj\u0105ce kolumny: " + String.join(", ", missing)
                    + ". Wymagane: " + String.join(", ", REQUIRED_COLUMNS));
        }

        Long tenantId = TenantContext.getTenantId();
        int added = 0;
        int updated = 0;
        List<RowError> errors = new ArrayList<>();

        // Data rows start at index 1
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNumber = i + 1; // 1-based for user display (header = row 1)

            try {
                String name = getCell(row, columnIndex, "name");
                String sku = getCell(row, columnIndex, "sku");
                String quantityStr = getCell(row, columnIndex, "quantity");
                String minQuantityStr = getCell(row, columnIndex, "min_quantity");
                String priceStr = getCell(row, columnIndex, "price");

                // Validate required fields
                if (name == null || name.isBlank()) {
                    errors.add(new RowError(rowNumber, "Nazwa produktu jest wymagana"));
                    continue;
                }
                if (sku == null || sku.isBlank()) {
                    errors.add(new RowError(rowNumber, "SKU jest wymagane"));
                    continue;
                }
                if (quantityStr == null || quantityStr.isBlank()) {
                    errors.add(new RowError(rowNumber, "Ilo\u015B\u0107 jest wymagana"));
                    continue;
                }
                if (minQuantityStr == null || minQuantityStr.isBlank()) {
                    errors.add(new RowError(rowNumber, "Minimalny stan jest wymagany"));
                    continue;
                }
                if (priceStr == null || priceStr.isBlank()) {
                    errors.add(new RowError(rowNumber, "Cena jest wymagana"));
                    continue;
                }

                int quantity;
                try {
                    quantity = Integer.parseInt(quantityStr.trim());
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNumber, "Nieprawid\u0142owa warto\u015B\u0107 ilo\u015Bci: " + quantityStr));
                    continue;
                }

                int minQuantity;
                try {
                    minQuantity = Integer.parseInt(minQuantityStr.trim());
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNumber, "Nieprawid\u0142owa warto\u015B\u0107 minimalnego stanu: " + minQuantityStr));
                    continue;
                }

                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr.trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNumber, "Nieprawid\u0142owa warto\u015B\u0107 ceny: " + priceStr));
                    continue;
                }

                // Validate positive values
                if (quantity < 0) {
                    errors.add(new RowError(rowNumber, "Ilo\u015B\u0107 nie mo\u017Ce by\u0107 ujemna"));
                    continue;
                }
                if (minQuantity < 0) {
                    errors.add(new RowError(rowNumber, "Minimalny stan nie mo\u017Ce by\u0107 ujemny"));
                    continue;
                }
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(new RowError(rowNumber, "Cena nie mo\u017Ce by\u0107 ujemna"));
                    continue;
                }

                // Optional fields
                String description = getCell(row, columnIndex, "description");
                String unit = getCell(row, columnIndex, "unit");

                // Upsert
                Optional<Product> existing = productRepository.findBySkuAndTenantId(sku.trim(), tenantId);
                if (existing.isPresent()) {
                    Product p = existing.get();
                    p.setName(name.trim());
                    if (description != null && !description.isBlank()) {
                        p.setDescription(description.trim());
                    }
                    if (unit != null && !unit.isBlank()) {
                        p.setUnit(unit.trim());
                    }
                    p.setQuantity(quantity);
                    p.setMinQuantity(minQuantity);
                    p.setPrice(price);
                    productRepository.save(p);
                    updated++;
                } else {
                    Product p = Product.builder()
                            .name(name.trim())
                            .sku(sku.trim())
                            .description(description != null ? description.trim() : null)
                            .unit(unit != null && !unit.isBlank() ? unit.trim() : "szt.")
                            .quantity(quantity)
                            .minQuantity(minQuantity)
                            .price(price)
                            .build();
                    productRepository.save(p);
                    added++;
                }
            } catch (Exception e) {
                log.warn("B\u0142\u0105d importu w wierszu {}: {}", rowNumber, e.getMessage());
                errors.add(new RowError(rowNumber, "Nieoczekiwany b\u0142\u0105d: " + e.getMessage()));
            }
        }

        return new ImportResult(added, updated, errors);
    }

    public byte[] generateTemplate() {
        String header = "name,sku,description,unit,quantity,min_quantity,price";
        String example = "Przyk\u0142adowy produkt,PRZ-001,Opis produktu,szt.,100,10,29.99";
        String example2 = "Inny produkt,PRZ-002,,szt.,50,5,15.50";
        String content = header + "\n" + example + "\n" + example2 + "\n";
        return content.getBytes(StandardCharsets.UTF_8);
    }

    // ─── CSV parsing ──────────────────────────────────────────────

    private List<String[]> parseCsv(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        String firstLine;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            firstLine = reader.readLine();
            if (firstLine == null) {
                return rows;
            }

            char separator = detectSeparator(firstLine);
            rows.add(splitCsvLine(firstLine, separator));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rows.add(splitCsvLine(line, separator));
            }
        }

        return rows;
    }

    private char detectSeparator(String headerLine) {
        long commaCount = headerLine.chars().filter(c -> c == ',').count();
        long semicolonCount = headerLine.chars().filter(c -> c == ';').count();
        return semicolonCount > commaCount ? ';' : ',';
    }

    private String[] splitCsvLine(String line, char separator) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == separator && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());

        return fields.toArray(new String[0]);
    }

    // ─── XLSX parsing ─────────────────────────────────────────────

    private List<String[]> parseXlsx(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                String[] fields = new String[row.getLastCellNum()];
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    fields[i] = getCellValueAsString(cell);
                }
                rows.add(fields);
            }
        }

        return rows;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf((long) cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private Map<String, Integer> buildColumnIndex(String[] header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null) {
                String key = header[i].trim().toLowerCase().replace(" ", "_");
                map.put(key, i);
            }
        }
        return map;
    }

    private String getCell(String[] row, Map<String, Integer> columnIndex, String columnName) {
        Integer idx = columnIndex.get(columnName);
        if (idx == null || idx >= row.length) return null;
        return row[idx];
    }
}
