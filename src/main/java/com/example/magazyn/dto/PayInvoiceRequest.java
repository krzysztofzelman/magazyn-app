package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;

public record PayInvoiceRequest(
    @NotBlank String paymentMethod,
    String paymentAccount
) {}
