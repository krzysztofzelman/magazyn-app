package com.example.magazyn.ksef.model.dto;

import java.time.LocalDateTime;

public record KSeFSessionResponse(
    Long id,
    String referenceNumber,
    Boolean isActive,
    LocalDateTime initiatedAt,
    LocalDateTime expiresAt,
    LocalDateTime refreshedAt,
    LocalDateTime lastUsedAt,
    String nip,
    String apiVersion,
    String errorMessage
) {}
