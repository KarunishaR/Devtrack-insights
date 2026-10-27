package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.RepoSuggestionSummaryDTO;

public interface OpenSourceSuggestionService {
    RepoSuggestionSummaryDTO suggestForUser(String username);
}
