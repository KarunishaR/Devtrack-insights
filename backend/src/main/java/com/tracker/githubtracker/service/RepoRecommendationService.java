package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.RepoSuggestionSummaryDTO;
import com.tracker.githubtracker.dto.SkillSummaryDTO;

public interface RepoRecommendationService {
    RepoSuggestionSummaryDTO suggestRepositories(SkillSummaryDTO skillSummary);
}
