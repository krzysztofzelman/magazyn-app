package com.example.magazyn.dto;

import com.example.magazyn.entity.ContractorType;

import java.time.LocalDateTime;

public class ContractorResponse {

    private Long id;
    private String name;
    private String taxId;
    private String address;
    private String email;
    private String phone;
    private ContractorType type;
    private Boolean active;
    private LocalDateTime createdAt;

    public ContractorResponse() {}

    public ContractorResponse(Long id, String name, String taxId, String address, String email,
                              String phone, ContractorType type, Boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.taxId = taxId;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.type = type;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public ContractorType getType() { return type; }
    public void setType(ContractorType type) { this.type = type; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
