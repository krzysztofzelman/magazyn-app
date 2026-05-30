package com.example.magazyn.dto;

import java.time.LocalDateTime;

public class TransferResponse {

    private String fromLocationCode;
    private String fromLocationName;
    private String toLocationCode;
    private String toLocationName;
    private Long productId;
    private String productName;
    private Double quantityMoved;
    private LocalDateTime timestamp;

    public TransferResponse() {}

    public TransferResponse(String fromLocationCode, String fromLocationName,
                            String toLocationCode, String toLocationName,
                            Long productId, String productName,
                            Double quantityMoved, LocalDateTime timestamp) {
        this.fromLocationCode = fromLocationCode;
        this.fromLocationName = fromLocationName;
        this.toLocationCode = toLocationCode;
        this.toLocationName = toLocationName;
        this.productId = productId;
        this.productName = productName;
        this.quantityMoved = quantityMoved;
        this.timestamp = timestamp;
    }

    public String getFromLocationCode() { return fromLocationCode; }
    public void setFromLocationCode(String fromLocationCode) { this.fromLocationCode = fromLocationCode; }

    public String getFromLocationName() { return fromLocationName; }
    public void setFromLocationName(String fromLocationName) { this.fromLocationName = fromLocationName; }

    public String getToLocationCode() { return toLocationCode; }
    public void setToLocationCode(String toLocationCode) { this.toLocationCode = toLocationCode; }

    public String getToLocationName() { return toLocationName; }
    public void setToLocationName(String toLocationName) { this.toLocationName = toLocationName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getQuantityMoved() { return quantityMoved; }
    public void setQuantityMoved(Double quantityMoved) { this.quantityMoved = quantityMoved; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
