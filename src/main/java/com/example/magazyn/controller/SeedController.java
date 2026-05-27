package com.example.magazyn.controller;

import com.example.magazyn.service.SeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/seed")
public class SeedController {

    private final SeedService seedService;

    public SeedController(SeedService seedService) {
        this.seedService = seedService;
    }

    @PostMapping("/locations")
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
