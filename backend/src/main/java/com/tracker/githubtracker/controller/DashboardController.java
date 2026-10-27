package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.DashboardDTO;
import com.tracker.githubtracker.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Everything at once: profile + skills + contributions + career
     * suggestions + repo suggestions. This is what the website calls
     * the moment a user opens their dashboard.
     * GET http://localhost:8080/api/github/{username}/dashboard
     */
    @GetMapping("/{username}/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard(@PathVariable String username) {
        return ResponseEntity.ok(dashboardService.getDashboard(username));
    }
}
