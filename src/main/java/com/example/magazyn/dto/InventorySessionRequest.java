package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InventorySessionRequest {

    @NotBlank(message = "Nazwa sesji jest wymagana")
    private String name;

    @NotNull(message = "ID magazynu jest wymagane")
    private Long warehouseId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
}
