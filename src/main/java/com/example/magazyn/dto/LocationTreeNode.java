package com.example.magazyn.dto;

import java.util.ArrayList;
import java.util.List;

public class LocationTreeNode {

    private Long id;
    private String code;
    private String name;
    private String type;
    private String description;
    private String barcode;
    private Integer capacity;
    private Integer occupied;
    private String zone;
    private Boolean isActive;
    private List<LocationTreeNode> children;

    public LocationTreeNode() {
        this.children = new ArrayList<>();
    }

    public LocationTreeNode(Long id, String code, String name, String type,
                            String description, String barcode, Integer capacity,
                            Integer occupied, String zone, Boolean isActive,
                            List<LocationTreeNode> children) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.description = description;
        this.barcode = barcode;
        this.capacity = capacity;
        this.occupied = occupied;
        this.zone = zone;
        this.isActive = isActive;
        this.children = children;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getOccupied() { return occupied; }
    public void setOccupied(Integer occupied) { this.occupied = occupied; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public List<LocationTreeNode> getChildren() { return children; }
    public void setChildren(List<LocationTreeNode> children) { this.children = children; }
}
