package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.CareerSuggestionDTO;
import com.tracker.githubtracker.dto.SkillSummaryDTO;

public interface CareerAdvisorService {
    CareerSuggestionDTO getCareerSuggestions(SkillSummaryDTO skillSummary);
}
