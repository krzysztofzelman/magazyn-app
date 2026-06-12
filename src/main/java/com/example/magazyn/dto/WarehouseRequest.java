package com.example.magazyn.dto;

public class WarehouseRequest {

    private String name;
    private String code;
    private Boolean isActive;

    public WarehouseRequest() {}

    public WarehouseRequest(String name, String code, Boolean isActive) {
        this.name = name;
        this.code = code;
        this.isActive = isActive;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
