package com.example.magazyn.controller;

import com.example.magazyn.dto.StockMovementRequest;
import com.example.magazyn.dto.StockMovementResponse;
import com.example.magazyn.dto.StockResponse;
import com.example.magazyn.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/{productId}/movement")
    public ResponseEntity<StockMovementResponse> addMovement(
            @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        StockMovementResponse response = stockService.addMovement(productId, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}/movements")
    public ResponseEntity<List<StockMovementResponse>> getMovements(@PathVariable Long productId) {
        List<StockMovementResponse> movements = stockService.getMovements(productId);
        return ResponseEntity.ok(movements);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long productId) {
        StockResponse stock = stockService.getStock(productId);
        return ResponseEntity.ok(stock);
    }
}
