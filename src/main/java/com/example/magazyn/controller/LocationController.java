package com.example.magazyn.controller;

import com.example.magazyn.dto.LocationRequest;
import com.example.magazyn.dto.LocationResponse;
import com.example.magazyn.dto.LocationStockResponse;
import com.example.magazyn.dto.LocationTreeNode;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.dto.TransferRequest;
import com.example.magazyn.dto.TransferResponse;
import com.example.magazyn.service.LabelService;
import com.example.magazyn.service.LocationService;
import com.example.magazyn.service.ProductService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Lokalizacje", description = "Zarz\u0105dzanie lokalizacjami magazynowymi")
@SecurityRequirement(name = "bearerAuth")
public class LocationController {

    private final LocationService locationService;
    private final ProductService productService;
    private final LabelService labelService;

    @Autowired
    public LocationController(LocationService locationService, ProductService productService,
                              LabelService labelService) {
        this.locationService = locationService;
        this.productService = productService;
        this.labelService = labelService;
    }

    @GetMapping
    @Operation(summary = "Pobierz wszystkie lokalizacje")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocationResponse>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @GetMapping("/tree")
    @Operation(summary = "Pobierz drzewo lokalizacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocationTreeNode>> getLocationTree() {
        return ResponseEntity.ok(locationService.getLocationTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pobierz lokalizacj\u0119 po ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LocationResponse> getLocationById(@PathVariable @Parameter(description = "ID lokalizacji") Long id) {
        return locationService.getLocationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/products")
    @Operation(summary = "Pobierz produkty w lokalizacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductResponse>> getProductsByLocation(@PathVariable @Parameter(description = "ID lokalizacji") Long id) {
        return ResponseEntity.ok(productService.getProductsByLocation(id));
    }

    @PostMapping
    @Operation(summary = "Utw\u00f3rz now\u0105 lokalizacj\u0119", description = "Wymaga roli ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody LocationRequest request) {
        LocationResponse created = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj lokalizacj\u0119", description = "Wymaga roli ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationResponse> updateLocation(@PathVariable @Parameter(description = "ID lokalizacji") Long id,
                                                            @Valid @RequestBody LocationRequest request) {
        LocationResponse updated = locationService.updateLocation(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usu\u0144 lokalizacj\u0119", description = "Wymaga roli ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLocation(@PathVariable @Parameter(description = "ID lokalizacji") Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stock")
    @Operation(summary = "Pobierz stan magazynowy w lokalizacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocationStockResponse>> getLocationStock(
            @PathVariable @Parameter(description = "ID lokalizacji") Long id) {
        return ResponseEntity.ok(locationService.getLocationStock(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Szukaj lokalizacji po kodzie kreskowym")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LocationResponse> searchLocationByBarcode(
            @RequestParam @Parameter(description = "Kod kreskowy") String barcode) {
        return locationService.getLocationByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/barcode-image")
    @Operation(summary = "Pobierz obraz kodu kreskowego lokalizacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getBarcodeImage(
            @PathVariable @Parameter(description = "ID lokalizacji") Long id) {
        byte[] image = locationService.getBarcodeImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping("/{id}/qr-image")
    @Operation(summary = "Pobierz obraz kodu QR lokalizacji")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getQrImage(
            @PathVariable @Parameter(description = "ID lokalizacji") Long id) {
        byte[] image = locationService.getQrImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Przenie\u015b produkt mi\u0119dzy lokalizacjami")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransferResponse> transferStock(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {
        TransferResponse response = locationService.transferStock(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/label-pdf")
    @Operation(summary = "Pobierz etykiet\u0119 PDF lokalizacji (A6)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getLocationLabelPdf(
            @PathVariable @Parameter(description = "ID lokalizacji") Long id) {
        byte[] pdf = labelService.generateLocationLabel(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"location-" + id + "-label.pdf\"")
                .body(pdf);
    }

    @GetMapping("/labels-pdf")
    @Operation(summary = "Pobierz etykiety PDF wielu lokalizacji (A4, 2x2)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getLocationLabelsPdf(
            @RequestParam @Parameter(description = "Lista ID lokalizacji, np. 1,2,3") List<Long> ids) {
        byte[] pdf = labelService.generateLocationLabels(ids);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"locations-labels.pdf\"")
                .body(pdf);
    }
}
