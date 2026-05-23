package com.example.magazyn.dto;

import java.time.LocalDateTime;

public class ProductResponse {

    private Long id;
    private String name;
    private String sku;
    private String description;
    private String unit;
    private LocalDateTime createdAt;

    public ProductResponse() {}

    public ProductResponse(Long id, String name, String sku, String description, String unit, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.unit = unit;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
