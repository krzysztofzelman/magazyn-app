package com.example.magazyn.dto;

import com.example.magazyn.entity.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LocationRequest {

    @NotBlank(message = "Kod lokalizacji jest wymagany")
    private String code;

    @NotBlank(message = "Nazwa lokalizacji jest wymagana")
    private String name;

    @NotNull(message = "Typ lokalizacji jest wymagany")
    private LocationType type;
    private Long parentId;
    private String description;
    private Integer capacity;
    private String zone;
    private Boolean isActive;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocationType getType() { return type; }
    public void setType(LocationType type) { this.type = type; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
