package com.libraryos.repository;

import com.libraryos.dto.DashboardMetricsDTO;

public interface DashboardRepository {
    DashboardMetricsDTO getMetrics();
}
