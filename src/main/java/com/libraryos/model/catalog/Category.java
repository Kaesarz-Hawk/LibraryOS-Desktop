package com.libraryos.model.catalog;

import java.time.LocalDateTime;

public class Category {
    private Long id;
    private Long parentId;
    private String nameEn;
    private String nameBn;
    private String code;
    private String description;
    private boolean archived = false;
    private LocalDateTime createdAt;

    public Category() {
    }

    public Category(Long id, String code, String nameEn, String nameBn, String description) {
        this.id = id;
        this.code = code;
        this.nameEn = nameEn;
        this.nameBn = nameBn;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getNameBn() {
        return nameBn;
    }

    public void setNameBn(String nameBn) {
        this.nameBn = nameBn;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDisplayName() {
        if (nameBn != null && !nameBn.isBlank()) {
            return nameEn + " (" + nameBn + ")";
        }
        return nameEn;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}

