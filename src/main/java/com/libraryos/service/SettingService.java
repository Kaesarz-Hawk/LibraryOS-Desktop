package com.libraryos.service;

import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.AppSettingRepository;

import java.util.Collections;
import java.util.Map;

public class SettingService {

    private final AppSettingRepository appSettingRepository;

    public SettingService(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    public String getSetting(String key, String defaultValue) {
        return appSettingRepository.getSetting(key).orElse(defaultValue);
    }

    public void updateSetting(String key, String value, String valueType) {
        PermissionGuard.requirePermission(PermissionCode.SETTINGS_MANAGE);
        Long userId = SessionManager.getCurrentUserId();
        appSettingRepository.setSetting(key, value, valueType, userId);
    }

    public Map<String, String> getAllSettings() {
        try {
            return appSettingRepository.getAllSettings();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}

