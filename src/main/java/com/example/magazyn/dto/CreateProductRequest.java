package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateProductRequest {

    @NotBlank(message = "Nazwa produktu jest wymagana")
    private String name;

    @NotBlank(message = "SKU jest wymagane")
    private String sku;

    private String description;

    @NotBlank(message = "Jednostka miary jest wymagana")
    private String unit;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
