package com.example.magazyn.ksef.model.dto;

import com.example.magazyn.ksef.model.enums.KSeFOperationType;
import java.time.LocalDateTime;

public record KSeFAuditLogResponse(
    Long id,
    KSeFOperationType operation,
    Long ksefInvoiceId,
    Long sessionId,
    String performedBy,
    String nip,
    String details,
    Boolean success,
    String errorMessage,
    String errorCode,
    Integer durationMs,
    String ipAddress,
    LocalDateTime createdAt
) {}
