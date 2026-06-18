package com.example.magazyn.ksef.model.dto;

public record KSeFSendInvoiceResponse(
    String ksefReferenceNumber,
    String status,
    String message,
    Long ksefInvoiceId
) {}
