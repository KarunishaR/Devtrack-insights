package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.GitHubProfileDTO;
import com.tracker.githubtracker.dto.RepositoryDTO;
import com.tracker.githubtracker.service.GitHubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Plain Java - no Lombok.
 */
@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    /**
     * Quick sanity-check endpoint. Doesn't call GitHub at all -
     * just proves the Spring Boot server itself is up and reachable.
     * GET http://localhost:8080/api/github/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "Backend is running"));
    }

    /**
     * Full profile lookup.
     * GET http://localhost:8080/api/github/{username}
     */
    @GetMapping("/{username}")
    public ResponseEntity<GitHubProfileDTO> getUserProfile(@PathVariable String username) {
        GitHubProfileDTO profile = gitHubService.getUserProfile(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * Repository list lookup.
     * GET http://localhost:8080/api/github/{username}/repos
     */
    @GetMapping("/{username}/repos")
    public ResponseEntity<List<RepositoryDTO>> getUserRepositories(@PathVariable String username) {
        List<RepositoryDTO> repos = gitHubService.getUserRepositories(username);
        return ResponseEntity.ok(repos);
    }
}
