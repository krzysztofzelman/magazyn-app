package com.example.magazyn.dto;

public class LocationResponse {

    private Long id;
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private String description;
    private String barcode;
    private String qrData;
    private Integer capacity;
    private Integer occupied;
    private String zone;
    private String rack;
    private String shelf;
    private Boolean isActive;

    public LocationResponse() {}

    public LocationResponse(Long id, String code, String name, String type,
                            Long parentId, String description, String barcode,
                            String qrData, Integer capacity, Integer occupied,
                            String zone, String rack, String shelf, Boolean isActive) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.parentId = parentId;
        this.description = description;
        this.barcode = barcode;
        this.qrData = qrData;
        this.capacity = capacity;
        this.occupied = occupied;
        this.zone = zone;
        this.rack = rack;
        this.shelf = shelf;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getQrData() { return qrData; }
    public void setQrData(String qrData) { this.qrData = qrData; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getOccupied() { return occupied; }
    public void setOccupied(Integer occupied) { this.occupied = occupied; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getRack() { return rack; }
    public void setRack(String rack) { this.rack = rack; }

    public String getShelf() { return shelf; }
    public void setShelf(String shelf) { this.shelf = shelf; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
