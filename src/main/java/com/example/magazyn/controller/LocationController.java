package com.example.magazyn.controller;

import com.example.magazyn.dto.LocationRequest;
import com.example.magazyn.dto.LocationResponse;
import com.example.magazyn.dto.LocationTreeNode;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.service.LocationService;
import com.example.magazyn.service.ProductService;
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
public class LocationController {

    private final LocationService locationService;
    private final ProductService productService;

    @Autowired
    public LocationController(LocationService locationService, ProductService productService) {
        this.locationService = locationService;
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocationResponse>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocationTreeNode>> getLocationTree() {
        return ResponseEntity.ok(locationService.getLocationTree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LocationResponse> getLocationById(@PathVariable Long id) {
        return locationService.getLocationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductResponse>> getProductsByLocation(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductsByLocation(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationResponse> createLocation(@RequestBody LocationRequest request) {
        LocationResponse created = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationResponse> updateLocation(@PathVariable Long id,
                                                            @RequestBody LocationRequest request) {
        LocationResponse updated = locationService.updateLocation(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
