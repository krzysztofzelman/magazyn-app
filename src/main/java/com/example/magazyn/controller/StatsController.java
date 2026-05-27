package com.example.magazyn.controller;

import com.example.magazyn.dto.StatsDashboardResponse;
import com.example.magazyn.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statystyki", description = "Statystyki dashboardu magazynu")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    @Autowired
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Pobierz dane dashboardu", description = "Zwraca zbiorcze statystyki: liczba produkt\u00f3w, sumy, statusy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StatsDashboardResponse> getDashboard() {
        StatsDashboardResponse dashboard = statsService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }
}
