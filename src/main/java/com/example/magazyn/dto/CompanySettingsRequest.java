package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanySettingsRequest(
    @NotBlank String name,
    String taxId,
    String address,
    String bankName,
    String bankAccount,
    String phone,
    String email
) {}
