package com.libraryos.model.inventory;

import java.time.LocalDateTime;

public class Shelf {
    private Long id;
    private String code;
    private String sectionName;
    private String rackNumber;
    private String shelfLevel;
    private String description;
    private LocalDateTime createdAt;

    public Shelf() {
    }

    public Shelf(Long id, String code, String sectionName, String rackNumber, String shelfLevel) {
        this.id = id;
        this.code = code;
        this.sectionName = sectionName;
        this.rackNumber = rackNumber;
        this.shelfLevel = shelfLevel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getRackNumber() {
        return rackNumber;
    }

    public void setRackNumber(String rackNumber) {
        this.rackNumber = rackNumber;
    }

    public String getShelfLevel() {
        return shelfLevel;
    }

    public void setShelfLevel(String shelfLevel) {
        this.shelfLevel = shelfLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDisplayLocation() {
        StringBuilder sb = new StringBuilder(code);
        if (sectionName != null && !sectionName.isBlank()) {
            sb.append(" (").append(sectionName);
            if (rackNumber != null && !rackNumber.isBlank()) {
                sb.append(", ").append(rackNumber);
            }
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDisplayLocation();
    }
}

