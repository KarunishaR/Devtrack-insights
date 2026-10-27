package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.ContributionSummaryDTO;

public interface ContributionService {
    /** year = null means "trailing 12 months" (GitHub's default). */
    ContributionSummaryDTO getContributionSummary(String username, Integer year);
}
