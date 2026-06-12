package com.example.magazyn.dto;

import java.math.BigDecimal;

public record InvoiceItemResponse(
    Long id,
    Long productId,
    String productName,
    String productSku,
    String productUnit,
    Integer quantity,
    BigDecimal unitPriceNet,
    BigDecimal vatRate,
    BigDecimal vatAmount,
    BigDecimal totalNet,
    BigDecimal totalGross
) {}
