package com.example.magazyn.ksef.model.dto;

import com.example.magazyn.ksef.model.enums.KSeFStatus;
import java.time.LocalDateTime;

public record KSeFInvoiceResponse(
    Long id,
    Long invoiceId,
    String invoiceNumber,
    String ksefReferenceNumber,
    KSeFStatus status,
    String ksefStatusCode,
    String ksefStatusMessage,
    Integer submissionAttempts,
    LocalDateTime lastSubmittedAt,
    String lastErrorMessage,
    String lastErrorCode,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
