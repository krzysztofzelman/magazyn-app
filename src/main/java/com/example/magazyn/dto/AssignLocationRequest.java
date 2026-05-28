package com.example.magazyn.dto;

import jakarta.validation.constraints.NotNull;

public class AssignLocationRequest {

    @NotNull(message = "ID lokalizacji jest wymagane")
    private Long locationId;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
}
