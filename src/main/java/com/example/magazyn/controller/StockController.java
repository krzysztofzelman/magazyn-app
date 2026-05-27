package com.example.magazyn.controller;

import com.example.magazyn.dto.StockMovementRequest;
import com.example.magazyn.dto.StockMovementResponse;
import com.example.magazyn.dto.StockResponse;
import com.example.magazyn.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stan magazynowy", description = "Operacje na stanach magazynowych i ruchach towaru")
@SecurityRequirement(name = "bearerAuth")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/{productId}/movement")
    @Operation(summary = "Dodaj ruch magazynowy", description = "Przyj\u0119cie lub wydanie towaru. USER mo\u017ce tylko przyj\u0119cia, ADMIN oba typy.")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #request.type.name() == 'PRZYJECIE')")
    public ResponseEntity<StockMovementResponse> addMovement(
            @PathVariable @Parameter(description = "ID produktu") Long productId,
            @Valid @RequestBody StockMovementRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        StockMovementResponse response = stockService.addMovement(productId, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}/movements")
    @Operation(summary = "Pobierz histori\u0119 ruch\u00f3w produktu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StockMovementResponse>> getMovements(@PathVariable @Parameter(description = "ID produktu") Long productId) {
        List<StockMovementResponse> movements = stockService.getMovements(productId);
        return ResponseEntity.ok(movements);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Pobierz stan magazynowy produktu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StockResponse> getStock(@PathVariable @Parameter(description = "ID produktu") Long productId) {
        StockResponse stock = stockService.getStock(productId);
        return ResponseEntity.ok(stock);
    }
}
