package com.example.magazyn.controller;

import com.example.magazyn.dto.AssignLocationRequest;
import com.example.magazyn.dto.BatchResponse;
import com.example.magazyn.dto.CreateProductRequest;
import com.example.magazyn.dto.ImportResult;
import com.example.magazyn.dto.ProductAvailabilityResponse;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.dto.UpdateProductRequest;
import com.example.magazyn.service.BatchService;
import com.example.magazyn.service.ImportService;
import com.example.magazyn.service.ProductService;
import com.example.magazyn.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Produkty", description = "Zarz\u0105dzanie produktami w magazynie")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;
    private final ImportService importService;
    private final BatchService batchService;
    private final ReservationService reservationService;

    @Autowired
    public ProductController(ProductService productService, ImportService importService,
                             BatchService batchService, ReservationService reservationService) {
        this.productService = productService;
        this.importService = importService;
        this.batchService = batchService;
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "Pobierz list\u0119 produkt\u00f3w", description = "Zwraca stron\u0119 produkt\u00f3w z opcj\u0105 wyszukiwania i sortowania")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") @Parameter(description = "Numer strony") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "Rozmiar strony") int size,
            @RequestParam(defaultValue = "name") @Parameter(description = "Pole sortowania") String sort,
            @RequestParam(required = false) @Parameter(description = "Szukana fraza (po nazwie lub SKU)") String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<ProductResponse> products = productService.getAllProducts(pageable, search);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pobierz produkt po ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable @Parameter(description = "ID produktu") Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Pobierz produkt po SKU")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable @Parameter(description = "Kod SKU") String sku) {
        return productService.getProductBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Utw\u00f3rz nowy produkt", description = "Wymaga roli ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj produkt", description = "Wymaga roli ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable @Parameter(description = "ID produktu") Long id,
                                                          @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usu\u0144 produkt", description = "Wymaga roli ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable @Parameter(description = "ID produktu") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    @Operation(summary = "Importuj produkty z pliku CSV/XLSX", description = "Wymaga roli ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportResult> importProducts(
            @RequestParam("file") @Parameter(description = "Plik CSV lub XLSX") MultipartFile file) {
        ImportResult result = importService.importProducts(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/import/template")
    @Operation(summary = "Pobierz szablon importu CSV")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] data = importService.generateTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"import-template.csv\"")
                .body(data);
    }

    @PatchMapping("/{id}/location")
    @Operation(summary = "Przypisz lokalizacj\u0119 do produktu", description = "Wymaga roli ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> assignLocation(@PathVariable @Parameter(description = "ID produktu") Long id,
                                                           @Valid @RequestBody AssignLocationRequest request) {
        ProductResponse updated = productService.assignLocation(id, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/batches")
    @Operation(summary = "Pobierz partie produktu", description = "Zwraca list\u0119 partii dla danego produktu z ilo\u015bciami")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<BatchResponse>> getProductBatches(
            @PathVariable @Parameter(description = "ID produktu") Long id) {
        return ResponseEntity.ok(batchService.getBatchesByProduct(id));
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Pobierz dost\u0119pno\u015b\u0107 produktu", description = "Zwraca ilo\u015b\u0107 ca\u0142kowit\u0105, zarezerwowan\u0105 i dost\u0119pn\u0105")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductAvailabilityResponse> getProductAvailability(
            @PathVariable @Parameter(description = "ID produktu") Long id) {
        return ResponseEntity.ok(reservationService.getProductAvailability(id));
    }
}
