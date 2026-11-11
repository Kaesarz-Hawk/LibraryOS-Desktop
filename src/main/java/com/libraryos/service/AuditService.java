package com.libraryos.service;

import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.model.audit.AuditLog;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String entityType, String entityId, String summary, String reason) {
        Long actorId = SessionManager.getCurrentUserId();
        try {
            auditLogRepository.insert(new AuditLog(actorId, action, entityType, entityId, summary, reason));
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    public List<AuditLog> getRecentLogs(int limit) {
        PermissionGuard.requirePermission(PermissionCode.AUDIT_VIEW);
        try {
            return auditLogRepository.findRecent(limit);
        } catch (Exception e) {
            log.warn("Failed to retrieve audit logs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<AuditLog> searchLogs(String query, String action, String entityType, int limit) {
        PermissionGuard.requirePermission(PermissionCode.AUDIT_VIEW);
        try {
            return auditLogRepository.search(query, action, entityType, limit);
        } catch (Exception e) {
            log.warn("Failed to search audit logs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

