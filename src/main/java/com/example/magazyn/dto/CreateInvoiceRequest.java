package com.example.magazyn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
    @NotBlank String buyerName,
    @NotBlank String buyerTaxId,
    @NotBlank String buyerAddress,
    @NotNull LocalDate saleDate,
    @NotNull LocalDate dueDate,
    String paymentMethod,
    String paymentAccount,
    String notes,
    @NotEmpty @Valid List<CreateInvoiceItemRequest> items
) {}
