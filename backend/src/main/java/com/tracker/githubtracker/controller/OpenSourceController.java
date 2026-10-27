package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.OpenSourceSummaryDTO;
import com.tracker.githubtracker.dto.RepoSuggestionSummaryDTO;
import com.tracker.githubtracker.service.OpenSourceContributionService;
import com.tracker.githubtracker.service.OpenSourceSuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class OpenSourceController {

    private final OpenSourceContributionService service;
    private final OpenSourceSuggestionService suggestionService;

    public OpenSourceController(OpenSourceContributionService service,
                                 OpenSourceSuggestionService suggestionService) {
        this.service = service;
        this.suggestionService = suggestionService;
    }

    /**
     * Every pull request this user has opened anywhere on GitHub, with
     * real current status (OPEN / MERGED / CLOSED).
     * GET http://localhost:8080/api/github/{username}/open-source-contributions
     */
    @GetMapping("/{username}/open-source-contributions")
    public ResponseEntity<OpenSourceSummaryDTO> getContributions(@PathVariable String username) {
        return ResponseEntity.ok(service.getContributions(username));
    }

    /**
     * Repositories to contribute to next, chosen using BOTH detected
     * skills and prior contribution history, each with a direct link
     * and a specific reason.
     * GET http://localhost:8080/api/github/{username}/open-source-suggestions
     */
    @GetMapping("/{username}/open-source-suggestions")
    public ResponseEntity<RepoSuggestionSummaryDTO> getSuggestions(@PathVariable String username) {
        return ResponseEntity.ok(suggestionService.suggestForUser(username));
    }
}
