package com.example.magazyn.ksef.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record KSeFInvoiceRequest(
    @NotBlank String invoiceNumber,
    @NotNull LocalDate issueDate,
    @NotNull LocalDate saleDate,
    LocalDate dueDate,
    @NotNull @Positive BigDecimal totalNet,
    @NotNull @Positive BigDecimal totalVat,
    @NotNull @Positive BigDecimal totalGross,
    @NotBlank String currency,
    @NotBlank String buyerNip,
    @NotBlank String buyerName,
    String buyerAddress,
    @NotBlank String sellerNip,
    @NotBlank String sellerName,
    String sellerAddress,
    String paymentMethod,
    String paymentAccount,
    List<KSeFInvoiceItemRequest> items,
    String notes
) {}
