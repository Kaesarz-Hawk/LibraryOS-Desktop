package com.libraryos.model.catalog;

import java.time.LocalDateTime;

public class Author {
    private Long id;
    private String nameEn;
    private String nameBn;
    private String biography;
    private boolean archived = false;
    private LocalDateTime createdAt;

    public Author() {
    }

    public Author(Long id, String nameEn, String nameBn, String biography) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameBn = nameBn;
        this.biography = biography;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
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

