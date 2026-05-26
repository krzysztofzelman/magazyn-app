package com.example.magazyn.repository;

public interface TopSellingProjection {
    Long getProductId();
    String getProductName();
    Long getTotalSold();
}
