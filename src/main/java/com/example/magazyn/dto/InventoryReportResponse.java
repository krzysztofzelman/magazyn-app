package com.example.magazyn.dto;

import java.math.BigDecimal;
import java.util.List;

public class InventoryReportResponse {

    private Long sessionId;
    private String sessionName;
    private String status;
    private List<InventoryItemResponse> items;
    private BigDecimal totalExpected;
    private BigDecimal totalCounted;
    private BigDecimal totalDifference;

    public InventoryReportResponse() {}

    public InventoryReportResponse(Long sessionId, String sessionName, String status,
                                   List<InventoryItemResponse> items, BigDecimal totalExpected,
                                   BigDecimal totalCounted, BigDecimal totalDifference) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.status = status;
        this.items = items;
        this.totalExpected = totalExpected;
        this.totalCounted = totalCounted;
        this.totalDifference = totalDifference;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<InventoryItemResponse> getItems() { return items; }
    public void setItems(List<InventoryItemResponse> items) { this.items = items; }

    public BigDecimal getTotalExpected() { return totalExpected; }
    public void setTotalExpected(BigDecimal totalExpected) { this.totalExpected = totalExpected; }

    public BigDecimal getTotalCounted() { return totalCounted; }
    public void setTotalCounted(BigDecimal totalCounted) { this.totalCounted = totalCounted; }

    public BigDecimal getTotalDifference() { return totalDifference; }
    public void setTotalDifference(BigDecimal totalDifference) { this.totalDifference = totalDifference; }
}
