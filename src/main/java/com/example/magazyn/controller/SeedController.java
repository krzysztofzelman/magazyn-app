package com.example.magazyn.controller;

import com.example.magazyn.service.SeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/locations")
    @Operation(summary = "Zasiej przyk\u0142adowe lokalizacje", description = "Wymaga roli ADMIN")
    public ResponseEntity<Map<String, Object>> seedLocations() {
        Map<String, Object> result = seedService.seedLocations();
        boolean success = (boolean) result.get("success");

        if (success) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
