package com.libraryos.repository;

import com.libraryos.model.audit.AppSetting;

import java.util.Map;
import java.util.Optional;

public interface AppSettingRepository {
    Optional<String> getSetting(String key);
    void setSetting(String key, String value, String valueType, Long updatedBy);
    Map<String, String> getAllSettings();
}
