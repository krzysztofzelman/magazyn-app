package com.example.magazyn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Zeskanowany kod do dodania do koszyka")
public class PendingScanRequest {
    @NotBlank(message = "Tryb skanowania jest wymagany")
    @Schema(description = "Tryb skanowania: PZ, WZ, TRANSFER, INVENTORY", example = "PZ")
    private String mode;

    @NotBlank(message = "Kod kreskowy jest wymagany")
    @Schema(description = "Zeskanowany kod kreskowy / SKU", example = "5901234567890")
    private String barcode;

    @NotNull(message = "Ilość jest wymagana")
    @Positive(message = "Ilość musi być dodatnia")
    @Schema(description = "Ilość (domyślnie 1)", example = "1")
    private Integer quantity;
}
