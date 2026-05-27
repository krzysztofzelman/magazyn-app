package com.example.magazyn.dto;

import java.util.ArrayList;
import java.util.List;

public class LocationTreeNode {

    private Long id;
    private String code;
    private String name;
    private String type;
    private String description;
    private List<LocationTreeNode> children;

    public LocationTreeNode() {
        this.children = new ArrayList<>();
    }

    public LocationTreeNode(Long id, String code, String name, String type,
                            String description, List<LocationTreeNode> children) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.description = description;
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

    public List<LocationTreeNode> getChildren() { return children; }
    public void setChildren(List<LocationTreeNode> children) { this.children = children; }
}
