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
import java.util.*;

/**
 * Suggests beginner-friendly open-source repos to contribute to, based
 * on the user's top detected languages, using GitHub's Search API with
 * the "good-first-issues" qualifier.
 *
 * Every suggestion is paired with a REASON generated dynamically from
 * real numbers (their skill percentage in that language + the repo's
 * actual star count and open-issue count) - not a fixed template, and
 * not an AI call - so it changes naturally as their skills or the
 * matched repos change.
 */
@Service
public class RepoRecommendationServiceImpl implements RepoRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RepoRecommendationServiceImpl.class);
    private static final int TOP_LANGUAGES_TO_USE = 2;
    private static final int RESULTS_PER_LANGUAGE = 5;

    private final RestTemplate restTemplate;
    private final SimpleTtlCache<String, RepoSuggestionSummaryDTO> cache =
            new SimpleTtlCache<>(Duration.ofMinutes(10).toMillis());

    @Value("${github.api.base-url}")
    private String baseUrl;

    public RepoRecommendationServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public RepoSuggestionSummaryDTO suggestRepositories(SkillSummaryDTO skillSummary) {
        if (skillSummary.getSkills() == null || skillSummary.getSkills().isEmpty()) {
            throw new GitHubApiException(
                    "Cannot suggest repositories - no detected languages to base suggestions on.");
        }

        List<SkillDTO> topSkills = skillSummary.getSkills().stream()
                .limit(TOP_LANGUAGES_TO_USE)
                .toList();

        // Cache key is just the top languages actually used in the search -
        // that's all the result depends on, so this correctly reuses the
        // cached result across pages within the same session.
        String cacheKey = topSkills.stream().map(SkillDTO::getLanguage).reduce("", (a, b) -> a + "|" + b);
        RepoSuggestionSummaryDTO cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<RepoSuggestionItemDTO> combined = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        for (SkillDTO skill : topSkills) {
            String language = skill.getLanguage();
            Map<String, String> params = new LinkedHashMap<>();
            params.put("q", "language:" + language + " good-first-issues:>0");
            params.put("sort", "stars");
            params.put("order", "desc");
            params.put("per_page", String.valueOf(RESULTS_PER_LANGUAGE));
            URI url = UrlUtil.buildUri(baseUrl, "/search/repositories", params);

            try {
                GitHubSearchResponseDTO result = restTemplate.getForObject(url, GitHubSearchResponseDTO.class);
                if (result != null && result.getItems() != null) {
                    for (RepositoryDTO repo : result.getItems()) {
                        if (seenNames.add(repo.getName())) {
                            String reason = buildReason(repo, skill);
                            combined.add(new RepoSuggestionItemDTO(repo, reason));
                        }
                    }
                }
            } catch (RestClientException ex) {
                log.warn("Repo search failed for language '{}': {}", language, ex.getMessage());
            }
        }

        List<String> languageNames = topSkills.stream().map(SkillDTO::getLanguage).toList();
        RepoSuggestionSummaryDTO result = new RepoSuggestionSummaryDTO(languageNames, combined);
        cache.put(cacheKey, result);
        return result;
    }

    /**
     * Builds a specific, data-grounded reason for this exact repo and
     * this exact user - two different users (or the same user after
     * their skills change) will get different reasons, since the
     * numbers plugged in are always freshly computed.
     */
    private String buildReason(RepositoryDTO repo, SkillDTO matchedSkill) {
        int stars = repo.getStars() == null ? 0 : repo.getStars();
        int openIssues = repo.getOpenIssues() == null ? 0 : repo.getOpenIssues();

        StringBuilder reason = new StringBuilder();
        reason.append("Written in ").append(matchedSkill.getLanguage())
                .append(", which makes up ").append(String.format("%.0f", matchedSkill.getPercentage()))
                .append("% of your repositories.");

        if (stars >= 1000) {
            reason.append(" This is a large, well-established project (")
                    .append(formatCount(stars)).append(" stars)");
        } else if (stars >= 100) {
            reason.append(" This is an actively maintained project (")
                    .append(formatCount(stars)).append(" stars)");
        } else {
            reason.append(" This is a smaller, growing project (")
                    .append(formatCount(stars)).append(" stars)");
        }

        if (openIssues > 0) {
            reason.append(" with ").append(openIssues).append(" open issue")
                    .append(openIssues == 1 ? "" : "s").append(" to pick from.");
        } else {
            reason.append(".");
        }

        return reason.toString();
    }

    private String formatCount(int count) {
        if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
