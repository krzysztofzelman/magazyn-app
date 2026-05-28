package com.example.magazyn.controller;

import com.example.magazyn.dto.LocationRequest;
import com.example.magazyn.dto.LocationResponse;
import com.example.magazyn.dto.LocationTreeNode;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.service.LocationService;
import com.example.magazyn.service.ProductService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Lokalizacje", description = "Zarz\u0105dzanie lokalizacjami magazynowymi")
@SecurityRequirement(name = "bearerAuth")
public class LocationController {

    private final LocationService locationService;
    private final ProductService productService;

    @Autowired
    public LocationController(LocationService locationService, ProductService productService) {
        this.locationService = locationService;
        this.productService = productService;
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
}
