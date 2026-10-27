package com.tracker.githubtracker.service;

import com.tracker.githubtracker.config.SimpleTtlCache;
import com.tracker.githubtracker.dto.GitHubProfileDTO;
import com.tracker.githubtracker.dto.RepositoryDTO;
import com.tracker.githubtracker.exception.GitHubApiException;
import com.tracker.githubtracker.exception.UnauthorizedException;
import com.tracker.githubtracker.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

/**
 * This is the class that replaces "Repository" in a normal CRUD app.
 * Instead of querying a database, it queries GitHub's public REST API
 * using RestTemplate and maps the JSON response straight into DTOs.
 *
 * No data is ever stored - every call hits GitHub live.
 * Plain Java - no Lombok.
 */
@Service
public class GitHubServiceImpl implements GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubServiceImpl.class);

    private final RestTemplate restTemplate;

    // Every page needs the profile (for the top bar) and several features need
    // the repo list, so caching these two removes the most repeated GitHub calls.
    private final SimpleTtlCache<String, GitHubProfileDTO> profileCache =
            new SimpleTtlCache<>(Duration.ofMinutes(5).toMillis());
    private final SimpleTtlCache<String, List<RepositoryDTO>> reposCache =
            new SimpleTtlCache<>(Duration.ofMinutes(5).toMillis());

    @Value("${github.api.base-url}")
    private String baseUrl;

    public GitHubServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public GitHubProfileDTO getUserProfile(String username) {
        return profileCache.getOrCompute(username.toLowerCase(), () -> fetchUserProfile(username));
    }

    @Override
    public List<RepositoryDTO> getUserRepositories(String username) {
        return reposCache.getOrCompute(username.toLowerCase(), () -> fetchUserRepositories(username));
    }

    private GitHubProfileDTO fetchUserProfile(String username) {
        String url = baseUrl + "/users/" + username;
        log.info("Calling GitHub API -> {}", url);

        try {
            return restTemplate.getForObject(url, GitHubProfileDTO.class);
        } catch (HttpClientErrorException ex) {
            throw translateClientError(ex, username);
        } catch (RestClientException ex) {
            throw new GitHubApiException("Failed to reach GitHub API: " + ex.getMessage(), ex);
        }
    }

    private List<RepositoryDTO> fetchUserRepositories(String username) {
        String url = baseUrl + "/users/" + username + "/repos?per_page=100&sort=updated";
        log.info("Calling GitHub API -> {}", url);

        try {
            RepositoryDTO[] repos = restTemplate.getForObject(url, RepositoryDTO[].class);
            return repos == null ? List.of() : List.of(repos);
        } catch (HttpClientErrorException ex) {
            throw translateClientError(ex, username);
        } catch (RestClientException ex) {
            throw new GitHubApiException("Failed to reach GitHub API: " + ex.getMessage(), ex);
        }
    }

    private RuntimeException translateClientError(HttpClientErrorException ex, String username) {
        HttpStatusCode status = ex.getStatusCode();

        // Full detail (status + GitHub's actual response body) is logged
        // server-side only - never returned to the client. This keeps
        // internal/upstream details from leaking while still giving you
        // everything needed to diagnose from the console.
        log.warn("GitHub API call failed for user '{}'. Status: {}. Body: {}",
                username, status.value(), ex.getResponseBodyAsString());

        if (status.value() == 404) {
            return new UserNotFoundException(username);
        }
        if (status.value() == 401) {
            return new UnauthorizedException(
                    "Request was rejected as unauthorized. This is usually caused by a network proxy or "
                            + "firewall intercepting the request before it reaches GitHub, rather than an issue "
                            + "with GitHub itself. Check your server console log line above for the exact "
                            + "upstream response body.");
        }
        if (status.value() == 403) {
            return new GitHubApiException(
                    "GitHub API rate limit exceeded, or the request was blocked. Unauthenticated requests are "
                            + "limited to 60/hour per IP address. Try again later.");
        }
        return new GitHubApiException("GitHub API returned an unexpected error: " + status.value());
    }
}
