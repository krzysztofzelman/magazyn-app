package com.example.magazyn.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Pozycja w koszyku oczekujących skanów")
public record PendingScanResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Tryb skanowania") String mode,
        @Schema(description = "Zeskanowany kod") String barcode,
        @Schema(description = "ID produktu") Long productId,
        @Schema(description = "Nazwa produktu") String productName,
        @Schema(description = "SKU produktu") String productSku,
        @Schema(description = "Jednostka") String productUnit,
        @Schema(description = "Ilość") Integer quantity,
        @Schema(description = "Kto zeskanował") String scannedBy,
        @Schema(description = "Czas skanowania") LocalDateTime createdAt
) {}
