package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class CreateProductRequest {

    @NotBlank(message = "Nazwa produktu jest wymagana")
    private String name;

    @NotBlank(message = "SKU jest wymagane")
    private String sku;

    private String description;

    @NotBlank(message = "Jednostka miary jest wymagana")
    private String unit;

    @PositiveOrZero(message = "Cena musi być większa lub równa 0")
    private BigDecimal price;

    @PositiveOrZero(message = "Minimalny stan musi być większy lub równy 0")
    private Integer minQuantity;

    private Boolean trackExpiry;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }

    public Boolean getTrackExpiry() { return trackExpiry; }
    public void setTrackExpiry(Boolean trackExpiry) { this.trackExpiry = trackExpiry; }
}
