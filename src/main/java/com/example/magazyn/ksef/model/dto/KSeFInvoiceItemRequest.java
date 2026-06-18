package com.example.magazyn.ksef.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record KSeFInvoiceItemRequest(
    @NotBlank String name,
    @NotBlank String unit,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @Positive BigDecimal unitPriceNet,
    @NotNull BigDecimal vatRate,
    @NotNull @Positive BigDecimal totalNet,
    @NotNull @Positive BigDecimal totalVat,
    @NotNull @Positive BigDecimal totalGross
) {}
