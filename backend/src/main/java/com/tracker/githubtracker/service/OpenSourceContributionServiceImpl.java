package com.tracker.githubtracker.service;

import com.tracker.githubtracker.config.SimpleTtlCache;
import com.tracker.githubtracker.dto.*;
import com.tracker.githubtracker.exception.GitHubApiException;
import com.tracker.githubtracker.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds every pull request a user has opened on repositories they do NOT
 * own - i.e. real open-source contributions - with live status:
 * OPEN, MERGED or CLOSED (closed without merging).
 *
 * Uses GitHub's Search API (is:pr author:{username}). PRs against the
 * user's own repositories are filtered out, since opening a PR on your
 * own project is not an open-source contribution.
 *
 * Fetches up to 300 results (3 pages of 100). Results are cached briefly
 * (single-flight) because both the Overview and Open Source pages, plus
 * the suggestion engine, all need this list.
 */
@Service
public class OpenSourceContributionServiceImpl implements OpenSourceContributionService {

    private static final Logger log = LoggerFactory.getLogger(OpenSourceContributionServiceImpl.class);
    private static final int PER_PAGE = 100;
    private static final int MAX_PAGES = 3;
    private static final int SUMMARY_MAX_LENGTH = 220;

    private final RestTemplate restTemplate;
    private final SimpleTtlCache<String, OpenSourceSummaryDTO> cache =
            new SimpleTtlCache<>(Duration.ofMinutes(5).toMillis());

    @Value("${github.api.base-url}")
    private String baseUrl;

    public OpenSourceContributionServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public OpenSourceSummaryDTO getContributions(String username) {
        return cache.getOrCompute(username.toLowerCase(), () -> fetchContributions(username));
    }

    private OpenSourceSummaryDTO fetchContributions(String username) {
        List<GitHubIssueSearchItemDTO> allItems = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("q", "is:pr author:" + username);
            params.put("sort", "created");
            params.put("order", "desc");
            params.put("per_page", String.valueOf(PER_PAGE));
            params.put("page", String.valueOf(page));
            URI url = UrlUtil.buildUri(baseUrl, "/search/issues", params);

            GitHubIssueSearchResponseDTO response;
            try {
                response = restTemplate.getForObject(url, GitHubIssueSearchResponseDTO.class);
            } catch (RestClientException ex) {
                if (page == 1) {
                    throw new GitHubApiException("Failed to fetch open source contributions: " + ex.getMessage(), ex);
                }
                log.warn("Stopped paginating PR search at page {} due to error: {}", page, ex.getMessage());
                break;
            }

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                break;
            }
            allItems.addAll(response.getItems());
            if (response.getItems().size() < PER_PAGE) {
                break;
            }
        }

        String ownerPrefix = username.toLowerCase() + "/";
        List<OpenSourceContributionDTO> contributions = new ArrayList<>();
        int open = 0, merged = 0, closed = 0;

        for (GitHubIssueSearchItemDTO item : allItems) {
            String repoName = extractRepoName(item.getRepositoryUrl());
            if (repoName != null && repoName.toLowerCase().startsWith(ownerPrefix)) {
                continue; // a PR on the user's own repository is not an open-source contribution
            }

            String status = determineStatus(item);
            switch (status) {
                case "MERGED" -> merged++;
                case "CLOSED" -> closed++;
                default -> open++;
            }

            contributions.add(new OpenSourceContributionDTO(
                    repoName,
                    repoName != null ? "https://github.com/" + repoName : null,
                    item.getTitle(),
                    item.getHtmlUrl(),
                    status,
                    buildSummary(item.getBody()),
                    item.getCreatedAt(),
                    item.getUpdatedAt()
            ));
        }

        log.info("Open source contributions for '{}': {} external PRs ({} merged, {} open, {} closed)",
                username, contributions.size(), merged, open, closed);

        return new OpenSourceSummaryDTO(contributions.size(), open, merged, closed, contributions);
    }

    /** repository_url looks like "https://api.github.com/repos/owner/repo" */
    private String extractRepoName(String repositoryUrl) {
        if (repositoryUrl == null) return null;
        String marker = "/repos/";
        int idx = repositoryUrl.indexOf(marker);
        if (idx == -1) return null;
        return repositoryUrl.substring(idx + marker.length());
    }

    private String determineStatus(GitHubIssueSearchItemDTO item) {
        if (item.getPullRequest() != null && item.getPullRequest().getMergedAt() != null) {
            return "MERGED";
        }
        if ("closed".equalsIgnoreCase(item.getState())) {
            return "CLOSED";
        }
        return "OPEN";
    }

    private String buildSummary(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        String cleaned = rawBody
                .replaceAll("(?s)<!--.*?-->", " ")
                .replaceAll("[#*_`>]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isBlank()) {
            return null;
        }
        if (cleaned.length() <= SUMMARY_MAX_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, SUMMARY_MAX_LENGTH).trim() + "...";
    }
}
