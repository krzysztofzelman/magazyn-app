package com.example.magazyn.ksef.model.dto;

import com.example.magazyn.ksef.model.enums.KSeFStatus;

public record KSeFStatusResponse(
    Long id,
    String invoiceNumber,
    KSeFStatus status,
    String ksefReferenceNumber,
    String ksefStatusCode,
    String ksefStatusMessage
) {}
