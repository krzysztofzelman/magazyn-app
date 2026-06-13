package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateInvoiceItemRequest(
    @NotBlank String productName,
    @NotBlank String productSku,
    @NotBlank String productUnit,
    @NotNull @Positive Integer quantity,
    @NotNull BigDecimal unitPriceNet,
    @NotNull BigDecimal vatRate
) {}
