package com.example.magazyn.dto;

import java.time.LocalDateTime;

public class InventorySessionResponse {

    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private String createdBy;
    private String status;
    private Long warehouseId;
    private String warehouseName;
    private Integer itemCount;

    public InventorySessionResponse() {}

    public InventorySessionResponse(Long id, String name, LocalDateTime createdAt,
                                    String createdBy, String status, Long warehouseId,
                                    String warehouseName, Integer itemCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.status = status;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.itemCount = itemCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
}
