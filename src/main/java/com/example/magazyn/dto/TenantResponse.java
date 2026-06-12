package com.example.magazyn.dto;

import java.time.LocalDateTime;

public class TenantResponse {

    private Long id;
    private String name;
    private String subdomain;
    private String plan;
    private Integer maxUsers;
    private Long userCount;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public TenantResponse() {}

    public TenantResponse(Long id, String name, String subdomain, String plan,
                          Integer maxUsers, Long userCount, Boolean isActive,
                          LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.subdomain = subdomain;
        this.plan = plan;
        this.maxUsers = maxUsers;
        this.userCount = userCount;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
