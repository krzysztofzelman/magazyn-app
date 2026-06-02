package com.example.magazyn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Zeskanowany kod do dodania do koszyka")
public class PendingScanRequest {
    @Schema(description = "Tryb skanowania: PZ, WZ, TRANSFER, INVENTORY", example = "PZ")
    private String mode;

    @Schema(description = "Zeskanowany kod kreskowy / SKU", example = "5901234567890")
    private String barcode;

    @Schema(description = "Ilość (domyślnie 1)", example = "1")
    private Integer quantity;
}
