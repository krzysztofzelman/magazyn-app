package com.example.magazyn.dto;

import com.example.magazyn.entity.ContractorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class ContractorRequest {

    @NotBlank(message = "Nazwa kontrahenta jest wymagana")
    private String name;

    @Pattern(regexp = "^\\d{10}$", message = "NIP musi zawierać 10 cyfr")
    private String taxId;

    private String address;

    private String email;

    private String phone;

    @NotNull(message = "Typ kontrahenta jest wymagany")
    private ContractorType type;

    @NotNull(message = "Status aktywności jest wymagany")
    private Boolean active;

    public @NotBlank(message = "Nazwa kontrahenta jest wymagana") String getName() {
        return name;
    }

    public void setName(@NotBlank(message = "Nazwa kontrahenta jest wymagana") String name) {
        this.name = name;
    }

    public @Pattern(regexp = "^\\d{10}$", message = "NIP musi zawierać 10 cyfr") String getTaxId() {
        return taxId;
    }

    public void setTaxId(@Pattern(regexp = "^\\d{10}$", message = "NIP musi zawierać 10 cyfr") String taxId) {
        this.taxId = taxId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ContractorType getType() {
        return type;
    }

    public void setType(ContractorType type) {
        this.type = type;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
