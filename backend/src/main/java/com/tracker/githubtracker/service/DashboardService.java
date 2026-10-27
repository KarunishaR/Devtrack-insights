package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.DashboardDTO;

public interface DashboardService {
    DashboardDTO getDashboard(String username);
}
