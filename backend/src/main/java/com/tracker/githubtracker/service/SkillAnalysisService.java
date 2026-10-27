package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.SkillSummaryDTO;

public interface SkillAnalysisService {
    SkillSummaryDTO getSkillSummary(String username);
}
