package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.ContributionSummaryDTO;
import com.tracker.githubtracker.service.ContributionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class ContributionController {

    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    /**
     * Day-wise AND month-wise contribution calendar.
     * GET http://localhost:8080/api/github/{username}/contributions
     * GET http://localhost:8080/api/github/{username}/contributions?year=2025
     */
    @GetMapping("/{username}/contributions")
    public ResponseEntity<ContributionSummaryDTO> getContributions(
            @PathVariable String username,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(contributionService.getContributionSummary(username, year));
    }
}
