package com.tracker.githubtracker.service;

import com.tracker.githubtracker.config.SimpleTtlCache;
import com.tracker.githubtracker.dto.*;
import com.tracker.githubtracker.exception.GitHubApiException;
import com.tracker.githubtracker.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Fetches GitHub's contribution calendar (the green squares graph) via
 * GraphQL - the only API that exposes this data. Supports an optional
 * specific year (for the frontend's year-switcher); omitting it falls
 * back to GitHub's default trailing-12-months view.
 *
 * REQUIRES github.api.token to be set - GraphQL always needs auth, even
 * for public data.
 */
@Service
public class ContributionServiceImpl implements ContributionService {

    private static final Logger log = LoggerFactory.getLogger(ContributionServiceImpl.class);
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RestTemplate restTemplate;
    private final SimpleTtlCache<String, ContributionSummaryDTO> cache =
            new SimpleTtlCache<>(Duration.ofMinutes(5).toMillis());

    @Value("${github.graphql.url}")
    private String graphqlUrl;

    @Value("${github.api.token:}")
    private String githubToken;

    private static final String QUERY = """
            query($userName: String!, $from: DateTime, $to: DateTime) {
              user(login: $userName) {
                contributionsCollection(from: $from, to: $to) {
                  contributionCalendar {
                    totalContributions
                    weeks {
                      contributionDays {
                        date
                        contributionCount
                      }
                    }
                  }
                }
              }
            }
            """;

    public ContributionServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ContributionSummaryDTO getContributionSummary(String username, Integer year) {
        String cacheKey = username.toLowerCase() + "|" + (year == null ? "default" : year);
        return cache.getOrCompute(cacheKey, () -> fetchContributionSummary(username, year));
    }

    private ContributionSummaryDTO fetchContributionSummary(String username, Integer year) {
        if (githubToken == null || githubToken.isBlank()) {
            throw new GitHubApiException(
                    "Contribution history requires a GitHub token. GraphQL (the only way to get this data) "
                            + "always requires authentication, even for public profiles. Set github.api.token "
                            + "in application.properties to enable this feature.");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", username);
        if (year != null) {
            variables.put("from", year + "-01-01T00:00:00Z");
            variables.put("to", year + "-12-31T23:59:59Z");
        } else {
            variables.put("from", null);
            variables.put("to", null);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("query", QUERY);
        body.put("variables", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(githubToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        GraphQLResponseDTO response;
        try {
            response = restTemplate.postForObject(graphqlUrl, request, GraphQLResponseDTO.class);
        } catch (RestClientException ex) {
            throw new GitHubApiException("Failed to reach GitHub GraphQL API: " + ex.getMessage(), ex);
        }

        if (response == null) {
            throw new GitHubApiException("GitHub GraphQL API returned an empty response.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            String errorSummary = response.getErrors().toString();
            log.warn("GitHub GraphQL returned errors for user '{}': {}", username, errorSummary);
            if (errorSummary.toLowerCase().contains("could not resolve to a user")) {
                throw new UserNotFoundException(username);
            }
            throw new GitHubApiException("GitHub GraphQL API returned an error. Check server logs for detail.");
        }

        if (response.getData() == null || response.getData().getUser() == null) {
            throw new UserNotFoundException(username);
        }

        ContributionCalendarRawDTO calendar = response.getData().getUser()
                .getContributionsCollection().getContributionCalendar();

        List<ContributionDayDTO> daily = new ArrayList<>();
        for (ContributionWeekDTO week : calendar.getWeeks()) {
            daily.addAll(week.getContributionDays());
        }

        Map<String, Integer> monthlyTotals = new LinkedHashMap<>();
        for (ContributionDayDTO day : daily) {
            String monthKey = LocalDate.parse(day.getDate()).format(MONTH_FORMAT);
            monthlyTotals.merge(monthKey, day.getCount() == null ? 0 : day.getCount(), Integer::sum);
        }

        List<MonthlyContributionDTO> monthly = new ArrayList<>();
        monthlyTotals.forEach((month, total) -> monthly.add(new MonthlyContributionDTO(month, total)));

        return new ContributionSummaryDTO(calendar.getTotalContributions(), daily, monthly);
    }
}
