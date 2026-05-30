package com.example.magazyn.dto;

public class InventorySessionRequest {

    private String name;
    private Long warehouseId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
}
