package com.libraryos.model.audit;

import java.time.LocalDateTime;

public class AppSetting {
    private String key;
    private String value;
    private String valueType;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public AppSetting() {
    }

    public AppSetting(String key, String value, String valueType) {
        this.key = key;
        this.value = value;
        this.valueType = valueType;
        this.updatedAt = LocalDateTime.now();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
