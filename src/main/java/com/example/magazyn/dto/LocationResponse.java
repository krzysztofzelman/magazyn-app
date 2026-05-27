package com.example.magazyn.dto;

public class LocationResponse {

    private Long id;
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private String description;

    public LocationResponse() {}

    public LocationResponse(Long id, String code, String name, String type,
                            Long parentId, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.parentId = parentId;
        this.description = description;
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
}
