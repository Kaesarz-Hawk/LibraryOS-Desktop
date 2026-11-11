package com.libraryos.repository;

import com.libraryos.model.audit.AuditLog;

import java.util.List;

public interface AuditLogRepository {
    void insert(AuditLog log);
    List<AuditLog> findRecent(int limit);
    List<AuditLog> findByEntityType(String entityType, int limit);
    List<AuditLog> search(String query, String action, String entityType, int limit);
}
