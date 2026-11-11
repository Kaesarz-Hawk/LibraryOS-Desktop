package com.libraryos.service;

import com.libraryos.core.security.PermissionGuard;
import com.libraryos.dto.DashboardMetricsDTO;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.DashboardRepository;

public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardMetricsDTO getMetrics() {
        PermissionGuard.requireAnyPermission(PermissionCode.DASHBOARD_STAFF_VIEW, PermissionCode.DASHBOARD_MEMBER_VIEW);
        return dashboardRepository.getMetrics();
    }
}

