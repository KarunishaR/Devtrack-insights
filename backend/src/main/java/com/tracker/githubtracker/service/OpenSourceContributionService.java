package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.OpenSourceSummaryDTO;

public interface OpenSourceContributionService {
    OpenSourceSummaryDTO getContributions(String username);
}
