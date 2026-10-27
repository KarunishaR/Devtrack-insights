package com.tracker.githubtracker.service;

import com.tracker.githubtracker.config.SimpleTtlCache;
import com.tracker.githubtracker.dto.RepositoryDTO;
import com.tracker.githubtracker.dto.SkillDTO;
import com.tracker.githubtracker.dto.SkillSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Builds a real skill set using GitHub's per-repo Languages API
 * (GET /repos/{owner}/{repo}/languages), which returns the actual byte
 * count of EVERY language in a repo - not just the single "primary"
 * language. A MERN project therefore contributes JavaScript, CSS and
 * HTML, weighted by real code volume.
 *
 * PERFORMANCE:
 *  - the per-repo calls run in parallel on a dedicated thread pool
 *  - the result is cached per username (single-flight), so the first
 *    page that needs skills pays the cost once and every other page
 *    (Overview, Skills, Career, Open Source) reuses it.
 */
@Service
public class SkillAnalysisServiceImpl implements SkillAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SkillAnalysisServiceImpl.class);
    private static final int MAX_REPOS_TO_ANALYZE = 15;

    private final GitHubService gitHubService;
    private final RestTemplate restTemplate;
    private final ExecutorService executor;
    private final SimpleTtlCache<String, SkillSummaryDTO> cache =
            new SimpleTtlCache<>(Duration.ofMinutes(10).toMillis());

    @Value("${github.api.base-url}")
    private String baseUrl;

    public SkillAnalysisServiceImpl(GitHubService gitHubService,
                                    RestTemplate restTemplate,
                                    @Qualifier("githubExecutor") ExecutorService executor) {
        this.gitHubService = gitHubService;
        this.restTemplate = restTemplate;
        this.executor = executor;
    }

    @Override
    public SkillSummaryDTO getSkillSummary(String username) {
        return cache.getOrCompute(username.toLowerCase(), () -> computeSkillSummary(username));
    }

    private SkillSummaryDTO computeSkillSummary(String username) {
        long startedAt = System.currentTimeMillis();

        List<RepositoryDTO> repos = gitHubService.getUserRepositories(username);

        List<RepositoryDTO> reposToAnalyze = repos.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getFork()))
                .filter(r -> r.getFullName() != null)
                .limit(MAX_REPOS_TO_ANALYZE)
                .toList();

        List<CompletableFuture<Map<String, Long>>> futures = reposToAnalyze.stream()
                .map(repo -> CompletableFuture.supplyAsync(() -> fetchRepoLanguages(repo.getFullName()), executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Map<String, Long> languageBytes = new LinkedHashMap<>();
        Map<String, Integer> languageRepoCounts = new LinkedHashMap<>();

        for (CompletableFuture<Map<String, Long>> future : futures) {
            Map<String, Long> repoLanguages = future.join();
            for (Map.Entry<String, Long> entry : repoLanguages.entrySet()) {
                languageBytes.merge(entry.getKey(), entry.getValue(), Long::sum);
                languageRepoCounts.merge(entry.getKey(), 1, Integer::sum);
            }
        }

        long totalBytes = languageBytes.values().stream().mapToLong(Long::longValue).sum();

        List<SkillDTO> skills = new ArrayList<>();
        for (Map.Entry<String, Long> entry : languageBytes.entrySet()) {
            double percentage = totalBytes == 0
                    ? 0.0
                    : Math.round((entry.getValue() * 10000.0 / totalBytes)) / 100.0;
            int repoCount = languageRepoCounts.getOrDefault(entry.getKey(), 0);
            skills.add(new SkillDTO(entry.getKey(), repoCount, percentage));
        }

        skills.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));

        String topLanguage = skills.isEmpty() ? null : skills.get(0).getLanguage();

        log.info("Skill analysis for '{}' took {}ms across {} repos",
                username, System.currentTimeMillis() - startedAt, reposToAnalyze.size());

        return new SkillSummaryDTO(topLanguage, reposToAnalyze.size(), skills);
    }

    private Map<String, Long> fetchRepoLanguages(String fullName) {
        String url = baseUrl + "/repos/" + fullName + "/languages";
        try {
            ResponseEntity<Map<String, Long>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Long>>() {});
            Map<String, Long> body = response.getBody();
            return body == null ? Map.of() : body;
        } catch (RestClientException ex) {
            // One repo failing (rate limit on that call, deleted repo...) must not break the whole result.
            log.warn("Could not fetch languages for repo '{}': {}", fullName, ex.getMessage());
            return Map.of();
        }
    }
}
