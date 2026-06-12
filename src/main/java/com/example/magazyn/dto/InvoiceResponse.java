package com.example.magazyn.dto;

import com.example.magazyn.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
    Long id,
    String number,
    Long documentId,
    String documentNumber,
    InvoiceStatus status,

    // Seller
    String sellerName,
    String sellerTaxId,
    String sellerAddress,
    String sellerBankAccount,

    // Buyer
    String buyerName,
    String buyerTaxId,
    String buyerAddress,

    // Dates
    LocalDate issueDate,
    LocalDate saleDate,
    LocalDate dueDate,

    // Payment
    String paymentMethod,
    String paymentAccount,

    // Totals
    BigDecimal totalNet,
    BigDecimal totalVat,
    BigDecimal totalGross,

    String notes,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime paidAt,
    LocalDateTime cancelledAt,

    List<InvoiceItemResponse> items
) {}
