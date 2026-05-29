package com.example.magazyn.controller;

import com.example.magazyn.service.SeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/seed")
@Tag(name = "Seed", description = "Zasiewanie danych testowych (tylko ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class SeedController {

    private final SeedService seedService;

    public SeedController(SeedService seedService) {
        this.seedService = seedService;
    }

    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Zasiej wszystkie dane testowe",
            description = "U\u017Cytkownicy, lokalizacje, produkty, kontrahenci, dokumenty PZ/WZ i rezerwacje. Wymaga roli ADMIN.")
    public ResponseEntity<Map<String, Object>> seedAll() {
        Map<String, Object> result = seedService.seedAll();
        boolean success = isSuccess(result);
        if (success) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Zasiej przyk\u0142adowych u\u017Cytkownik\u00F3w",
            description = "admin (ROLE_ADMIN) i magazynier (ROLE_USER). Wymaga roli ADMIN.")
    public ResponseEntity<Map<String, Object>> seedUsers() {
        Map<String, Object> result = seedService.seedUsers();
        return isSuccess(result) ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Zasiej przyk\u0142adowe lokalizacje",
            description = "Hierarchia magazyn\u00F3w, rega\u0142\u00F3w, p\u00F3\u0142ek i miejsc. Wymaga roli ADMIN.")
    public ResponseEntity<Map<String, Object>> seedLocations() {
        Map<String, Object> result = seedService.seedLocations();
        return isSuccess(result) ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Zasiej przyk\u0142adowe produkty",
            description = "15 produkt\u00F3w z r\u00F3\u017Cnymi jednostkami, cenami i \u015Bledzeniem partii. Wymaga roli ADMIN.")
    public ResponseEntity<Map<String, Object>> seedProducts() {
        Map<String, Object> result = seedService.seedProducts();
        return isSuccess(result) ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/contractors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Zasiej przyk\u0142adowych kontrahent\u00F3w",
            description = "3 dostawc\u00F3w i 3 odbiorc\u00F3w. Wymaga roli ADMIN.")
    public ResponseEntity<Map<String, Object>> seedContractors() {
        Map<String, Object> result = seedService.seedContractors();
        return isSuccess(result) ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Zasiej przyk\u0142adowe dokumenty PZ/WZ",
            description = "3 dokumenty PZ (przych\u00F3d) i 2 dokumenty WZ (rozch\u00F3d) z r\u00F3\u017Cnymi produktami i partiami. Wymaga roli ADMIN.")
    public ResponseEntity<Map<String, Object>> seedDocuments(Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        Map<String, Object> result = seedService.seedDocuments(username);
        return isSuccess(result) ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Zasiej przyk\u0142adowe rezerwacje",
            description = "2 rezerwacje na wybrane produkty. Wymaga roli ADMIN.")
    public ResponseEntity<Map<String, Object>> seedReservations(Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        Map<String, Object> result = seedService.seedReservations(username);
        return isSuccess(result) ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    private boolean isSuccess(Map<String, Object> result) {
        return Boolean.TRUE.equals(result.get("success"));
    }
}
