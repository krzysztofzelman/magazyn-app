package com.example.magazyn.dto;

import java.time.LocalDateTime;

public class WarehouseResponse {

    private Long id;
    private String name;
    private String code;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public WarehouseResponse() {}

    public WarehouseResponse(Long id, String name, String code, Boolean isActive,
                             LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
