package com.example.magazyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterTenantRequest {

    @NotBlank(message = "Nazwa firmy jest wymagana")
    @Size(min = 2, max = 100, message = "Nazwa firmy musi mie\u0107 2-100 znak\u00F3w")
    private String companyName;

    @NotBlank(message = "Subdomena jest wymagana")
    @Size(min = 3, max = 50, message = "Subdomena musi mie\u0107 3-50 znak\u00F3w")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomena mo\u017Ce zawiera\u0107 tylko ma\u0142e litery, cyfry i my\u015Blniki")
    private String subdomain;

    @NotBlank(message = "Nazwa u\u017Cytkownika jest wymagana")
    @Size(min = 3, max = 50, message = "Nazwa u\u017Cytkownika musi mie\u0107 3-50 znak\u00F3w")
    private String adminUsername;

    @NotBlank(message = "Has\u0142o jest wymagane")
    @Size(min = 6, max = 100, message = "Has\u0142o musi mie\u0107 co najmniej 6 znak\u00F3w")
    private String adminPassword;

    private String adminEmail;

    public RegisterTenantRequest() {}

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
}
