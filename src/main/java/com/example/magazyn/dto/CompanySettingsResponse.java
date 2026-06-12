package com.example.magazyn.dto;

import java.time.LocalDateTime;

public record CompanySettingsResponse(
    Long id,
    String name,
    String taxId,
    String address,
    String bankName,
    String bankAccount,
    String phone,
    String email,
    LocalDateTime updatedAt
) {}
