package com.example.magazyn.dto;

public class TenantRegistrationResponse {

    private String message;
    private String username;
    private String tenantName;
    private String subdomain;

    public TenantRegistrationResponse() {}

    public TenantRegistrationResponse(String message, String username,
                                      String tenantName, String subdomain) {
        this.message = message;
        this.username = username;
        this.tenantName = tenantName;
        this.subdomain = subdomain;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }
}
