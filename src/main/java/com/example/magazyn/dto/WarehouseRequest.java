package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WarehouseRequest {

    @NotBlank(message = "Nazwa magazynu jest wymagana")
    @Size(max = 100, message = "Nazwa mo\u017Ce mie\u0107 maksymalnie 100 znak\u00F3w")
    private String name;

    @NotBlank(message = "Kod magazynu jest wymagany")
    @Size(min = 1, max = 20, message = "Kod musi mie\u0107 od 1 do 20 znak\u00F3w")
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
