package com.tracker.githubtracker.service;

import com.tracker.githubtracker.config.SimpleTtlCache;
import com.tracker.githubtracker.dto.ActivityItemDTO;
import com.tracker.githubtracker.dto.GitHubEventDTO;
import com.tracker.githubtracker.exception.GitHubApiException;
import com.tracker.githubtracker.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * Turns GitHub's public Events API into a plain-English activity feed -
 * "what did this person actually do" rather than just a contribution
 * count.
 *
 * PAGINATION: GitHub's Events API caps out at roughly the last 90 days
 * OR ~300 events, whichever comes first - but a single page only
 * returns 100. Fetching just page 1 (the old behavior) meant highly
 * active users only had their most recent few days/weeks covered, so
 * hovering slightly-older-but-still-within-90-days calendar squares
 * incorrectly showed "not available". Fetching up to 3 pages (300
 * events) gets as close to GitHub's real limit as the API allows.
 */
@Service
public class ActivityServiceImpl implements ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);
    private static final int PER_PAGE = 100;
    private static final int MAX_PAGES = 3;

    private final RestTemplate restTemplate;
    private final ExecutorService executor;
    private final SimpleTtlCache<String, List<ActivityItemDTO>> cache =
            new SimpleTtlCache<>(Duration.ofMinutes(5).toMillis());

    @Value("${github.api.base-url}")
    private String baseUrl;

    public ActivityServiceImpl(RestTemplate restTemplate, @Qualifier("githubExecutor") ExecutorService executor) {
        this.restTemplate = restTemplate;
        this.executor = executor;
    }

    @Override
    public List<ActivityItemDTO> getRecentActivity(String username) {
        return cache.getOrCompute(username.toLowerCase(), () -> fetchActivity(username));
    }

    private List<ActivityItemDTO> fetchActivity(String username) {
        // All pages are requested at the same time instead of one after another.
        List<CompletableFuture<GitHubEventDTO[]>> futures = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            final int pageNumber = page;
            futures.add(CompletableFuture.supplyAsync(() -> fetchPage(username, pageNumber), executor));
        }

        List<GitHubEventDTO> allEvents = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            GitHubEventDTO[] events;
            try {
                events = futures.get(i).join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (i == 0) {
                    // The first page failing means the whole feed failed (e.g. unknown user).
                    if (cause instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    throw new GitHubApiException("Failed to fetch activity: " + cause.getMessage(), cause);
                }
                log.warn("Activity page {} failed, continuing with what we have: {}", i + 1, cause.getMessage());
                continue;
            }
            if (events != null) {
                allEvents.addAll(Arrays.asList(events));
            }
        }

        List<ActivityItemDTO> result = new ArrayList<>();
        for (GitHubEventDTO event : allEvents) {
            result.add(toActivityItem(event));
        }
        return result;
    }

    private GitHubEventDTO[] fetchPage(String username, int page) {
        String url = baseUrl + "/users/" + username + "/events/public?per_page=" + PER_PAGE + "&page=" + page;
        try {
            return restTemplate.getForObject(url, GitHubEventDTO[].class);
        } catch (HttpClientErrorException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 404) {
                throw new UserNotFoundException(username);
            }
            throw new GitHubApiException("GitHub API returned an error while fetching activity: " + status.value());
        } catch (RestClientException ex) {
            throw new GitHubApiException("Failed to reach GitHub API: " + ex.getMessage(), ex);
        }
    }

    private ActivityItemDTO toActivityItem(GitHubEventDTO event) {
        String repoName = event.getRepo() != null ? event.getRepo().getName() : "unknown repository";
        String date = event.getCreatedAt() != null && event.getCreatedAt().length() >= 10
                ? event.getCreatedAt().substring(0, 10)
                : null;

        String description = describe(event, repoName);

        return new ActivityItemDTO(date, event.getCreatedAt(), event.getType(), repoName, description);
    }

    @SuppressWarnings("unchecked")
    private String describe(GitHubEventDTO event, String repoName) {
        String type = event.getType() == null ? "" : event.getType();
        Map<String, Object> payload = event.getPayload() == null ? Map.of() : event.getPayload();

        return switch (type) {
            case "PushEvent" -> {
                Object commits = payload.get("commits");
                int commitCount = (commits instanceof List<?> list) ? list.size() : 0;
                yield "Pushed " + commitCount + " commit" + (commitCount == 1 ? "" : "s") + " to " + repoName;
            }
            case "PullRequestEvent" -> {
                Object action = payload.get("action");
                yield capitalize(String.valueOf(action)) + " a pull request in " + repoName;
            }
            case "IssuesEvent" -> {
                Object action = payload.get("action");
                yield capitalize(String.valueOf(action)) + " an issue in " + repoName;
            }
            case "IssueCommentEvent" -> "Commented on an issue in " + repoName;
            case "WatchEvent" -> "Starred " + repoName;
            case "ForkEvent" -> "Forked " + repoName;
            case "CreateEvent" -> {
                Object refType = payload.get("ref_type");
                yield "Created a new " + refType + " in " + repoName;
            }
            case "DeleteEvent" -> {
                Object refType = payload.get("ref_type");
                yield "Deleted a " + refType + " in " + repoName;
            }
            case "ReleaseEvent" -> "Published a release in " + repoName;
            case "PublicEvent" -> "Made " + repoName + " public";
            default -> type.isBlank() ? "Activity in " + repoName : type + " in " + repoName;
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank() || s.equals("null")) return "Updated";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
