package com.tracker.githubtracker.service;

import com.tracker.githubtracker.config.SimpleTtlCache;
import com.tracker.githubtracker.dto.*;
import com.tracker.githubtracker.exception.GitHubApiException;
import com.tracker.githubtracker.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * Suggests open-source repositories to contribute to, personalised from
 * four real signals about THIS user (so two people with similar
 * languages still get different results):
 *
 *   1. PRIOR CONTRIBUTIONS - repos they already opened pull requests on.
 *      We look those repos up (topics + language) and suggest similar
 *      projects. This is the strongest signal.
 *   2. THEIR OWN REPO TOPICS - topics (react, nodejs, ...) that appear
 *      across their own repositories.
 *   3. THEIR LANGUAGE MIX - weighted by real code percentages from the
 *      skill analysis, using a different sort order per language so the
 *      result isn't just "the most-starred repo in language X".
 *   4. THEIR EXPERIENCE - number of merged PRs. Newcomers get smaller,
 *      welcoming projects with "good first issue" work; people with
 *      several merged PRs get larger projects with "help wanted" work.
 *
 * Repos they've already contributed to, and repos they own, are never
 * suggested. Every suggestion carries the repo's direct link/description
 * plus a specific reason naming which signal produced it.
 *
 * All searches run in parallel and the result is cached per user.
 */
@Service
public class OpenSourceSuggestionServiceImpl implements OpenSourceSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(OpenSourceSuggestionServiceImpl.class);

    private static final int PER_QUERY = 6;
    private static final int MAX_SUGGESTIONS = 12;
    private static final int MAX_LANGUAGE_PLANS = 3;
    private static final int MAX_TOPIC_PLANS = 2;
    private static final int MAX_PRIOR_REPOS = 3;

    /** "Languages" GitHub reports that are build/config files, not skills worth searching by. */
    private static final Set<String> SKIP_LANGUAGES = Set.of(
            "Makefile", "CMake", "Dockerfile", "M4", "Roff", "Batchfile", "TeX", "Meson", "Nix", "Jinja", "Gnuplot");

    /** Topics too generic to say anything about what someone works on. */
    private static final Set<String> NOISE_TOPICS = Set.of(
            "project", "projects", "college", "student", "students", "assignment", "learning", "practice",
            "beginner", "beginners", "first-contributions", "open-source", "opensource", "good-first-issue",
            "help-wanted", "tutorial", "demo", "example", "test", "sample", "portfolio", "website", "app",
            "webapp", "frontend", "backend", "fullstack");

    private enum Signal { PRIOR, TOPIC, LANGUAGE }

    private record Plan(Signal signal, String query, String sort, String detail, String issueLabel) {}

    private record PlanResult(Plan plan, List<RepositoryDTO> repos, boolean failed) {}

    private final SkillAnalysisService skillAnalysisService;
    private final OpenSourceContributionService contributionService;
    private final GitHubService gitHubService;
    private final RestTemplate restTemplate;
    private final ExecutorService executor;
    private final ExecutorService orchestrator;

    private final SimpleTtlCache<String, RepoSuggestionSummaryDTO> cache =
            new SimpleTtlCache<>(Duration.ofMinutes(10).toMillis());

    @Value("${github.api.base-url}")
    private String baseUrl;

    public OpenSourceSuggestionServiceImpl(SkillAnalysisService skillAnalysisService,
                                           OpenSourceContributionService contributionService,
                                           GitHubService gitHubService,
                                           RestTemplate restTemplate,
                                           @Qualifier("githubExecutor") ExecutorService executor,
                                           @Qualifier("orchestratorExecutor") ExecutorService orchestrator) {
        this.skillAnalysisService = skillAnalysisService;
        this.contributionService = contributionService;
        this.gitHubService = gitHubService;
        this.restTemplate = restTemplate;
        this.executor = executor;
        this.orchestrator = orchestrator;
    }

    @Override
    public RepoSuggestionSummaryDTO suggestForUser(String username) {
        return cache.getOrCompute(username.toLowerCase(), () -> compute(username));
    }

    private RepoSuggestionSummaryDTO compute(String username) {
        long startedAt = System.currentTimeMillis();

        // The three inputs don't depend on each other, so fetch them at the same time
        // (previously: skills, THEN PRs, THEN repos - each waiting for the one before).
        CompletableFuture<SkillSummaryDTO> skillsFuture =
                CompletableFuture.supplyAsync(() -> skillAnalysisService.getSkillSummary(username), orchestrator);
        CompletableFuture<OpenSourceSummaryDTO> priorFuture =
                CompletableFuture.supplyAsync(() -> loadPriorContributions(username), orchestrator);
        CompletableFuture<List<RepositoryDTO>> ownReposFuture =
                CompletableFuture.supplyAsync(() -> loadOwnRepositories(username), orchestrator);

        SkillSummaryDTO skills = awaitSkills(skillsFuture);
        if (skills.getSkills() == null || skills.getSkills().isEmpty()) {
            throw new GitHubApiException(
                    "Cannot suggest repositories yet - no languages were detected in this profile's repositories.");
        }
        OpenSourceSummaryDTO prior = priorFuture.join();
        List<RepositoryDTO> ownRepos = ownReposFuture.join();

        // ---- experience level drives how big / what kind of projects we suggest ----
        int merged = prior == null ? 0 : prior.getMergedCount();
        boolean experienced = merged >= 5;
        String issueQualifier = experienced ? "help-wanted-issues:>0" : "good-first-issues:>0";
        String issueLabel = experienced ? "help wanted" : "good first issue";
        String starRange = merged >= 5 ? "500..60000" : (merged >= 1 ? "100..20000" : "30..8000");
        String suffix = " archived:false stars:" + starRange + " pushed:>" + LocalDate.now().minusDays(120);

        Set<String> skillLanguagesLower = new HashSet<>();
        for (SkillDTO s : skills.getSkills()) {
            skillLanguagesLower.add(s.getLanguage().toLowerCase());
        }

        Set<String> excluded = new HashSet<>();
        if (prior != null && prior.getContributions() != null) {
            for (OpenSourceContributionDTO c : prior.getContributions()) {
                if (c.getRepoName() != null) {
                    excluded.add(c.getRepoName().toLowerCase());
                }
            }
        }
        String ownerPrefix = username.toLowerCase() + "/";

        List<Plan> plans = new ArrayList<>();
        List<String> basis = new ArrayList<>();
        Set<String> usedTopics = new HashSet<>();

        // ---- Signal 1: repos they already contributed to ----
        Map<String, String> priorRepos = recentContributedRepos(prior);
        Map<String, CompletableFuture<RepositoryDTO>> detailFutures = new LinkedHashMap<>();
        for (String repoName : priorRepos.keySet()) {
            detailFutures.put(repoName, CompletableFuture.supplyAsync(() -> fetchRepoDetails(repoName), executor));
        }
        for (Map.Entry<String, CompletableFuture<RepositoryDTO>> entry : detailFutures.entrySet()) {
            String repoName = entry.getKey();
            RepositoryDTO detail = entry.getValue().join();
            String status = priorRepos.get(repoName).toLowerCase();

            String topic = detail == null ? null : firstUsefulTopic(detail.getTopics(), skillLanguagesLower, usedTopics);
            String criterion;
            String shared;
            if (topic != null) {
                criterion = "topic:" + topic;
                shared = "shares the '" + topic + "' topic";
                usedTopics.add(topic);
            } else if (detail != null && detail.getLanguage() != null && !SKIP_LANGUAGES.contains(detail.getLanguage())) {
                criterion = "language:" + quoteIfNeeded(detail.getLanguage());
                shared = "is also written in " + detail.getLanguage();
            } else {
                continue;
            }
            plans.add(new Plan(Signal.PRIOR, criterion + " " + issueQualifier + suffix, "stars",
                    "Similar to " + repoName + " (you opened a pull request there, status: " + status
                            + ") - this project " + shared + ".",
                    issueLabel));
            basis.add("your PR in " + repoName);
        }

        // ---- Signal 2: topics across their own repositories ----
        Map<String, Integer> topicCounts = new HashMap<>();
        for (RepositoryDTO repo : ownRepos) {
            if (Boolean.TRUE.equals(repo.getFork()) || repo.getTopics() == null) continue;
            for (String topic : repo.getTopics()) {
                topicCounts.merge(topic, 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> rankedTopics = new ArrayList<>();
        for (Map.Entry<String, Integer> e : topicCounts.entrySet()) {
            if (isUsefulTopic(e.getKey(), skillLanguagesLower) && !usedTopics.contains(e.getKey())) {
                rankedTopics.add(e);
            }
        }
        rankedTopics.sort((a, b) -> {
            int byCount = Integer.compare(b.getValue(), a.getValue());
            return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
        });
        for (Map.Entry<String, Integer> e : rankedTopics.stream().limit(MAX_TOPIC_PLANS).toList()) {
            int n = e.getValue();
            plans.add(new Plan(Signal.TOPIC, "topic:" + e.getKey() + " " + issueQualifier + suffix, "stars",
                    "Your own repositories are tagged '" + e.getKey() + "' (" + n + (n == 1 ? " repo" : " repos")
                            + "), and this project is active in the same area.",
                    issueLabel));
            basis.add("topic: " + e.getKey());
        }

        // ---- Signal 3: language mix, weighted by real code percentages ----
        String[] sorts = {"stars", "updated", "help-wanted-issues"};
        String topEligibleLanguage = null;
        int languagePlans = 0;
        for (SkillDTO s : skills.getSkills()) {
            if (languagePlans >= MAX_LANGUAGE_PLANS) break;
            String lang = s.getLanguage();
            if (SKIP_LANGUAGES.contains(lang) || s.getPercentage() < 2.0) continue;
            if (topEligibleLanguage == null) topEligibleLanguage = lang;
            plans.add(new Plan(Signal.LANGUAGE, "language:" + quoteIfNeeded(lang) + " " + issueQualifier + suffix,
                    sorts[languagePlans % sorts.length],
                    "Matches your " + lang + " experience (" + Math.round(s.getPercentage())
                            + "% of the code across your repositories).",
                    issueLabel));
            basis.add(lang);
            languagePlans++;
        }

        if (plans.isEmpty()) {
            throw new GitHubApiException("Not enough information on this profile to build suggestions yet.");
        }

        // ---- run every search at the same time ----
        List<CompletableFuture<PlanResult>> futures = plans.stream()
                .map(plan -> CompletableFuture.supplyAsync(() -> runPlan(plan), executor))
                .toList();
        List<PlanResult> results = futures.stream().map(CompletableFuture::join).toList();

        // ---- merge round-robin so every signal is represented, skipping repeats ----
        List<RepoSuggestionItemDTO> suggestions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < PER_QUERY && suggestions.size() < MAX_SUGGESTIONS; i++) {
            for (PlanResult result : results) {
                if (suggestions.size() >= MAX_SUGGESTIONS) break;
                if (i >= result.repos().size()) continue;
                tryAdd(result.repos().get(i), result.plan(), suggestions, seen, excluded, ownerPrefix);
            }
        }

        boolean allFailed = !results.isEmpty() && results.stream().allMatch(PlanResult::failed);

        // ---- if the filtered searches found very little, relax them once ----
        if (suggestions.size() < 3 && topEligibleLanguage != null) {
            Plan relaxed = new Plan(Signal.LANGUAGE,
                    "language:" + quoteIfNeeded(topEligibleLanguage) + " " + issueQualifier + " archived:false stars:>20",
                    "stars",
                    "Matches your " + topEligibleLanguage + " experience.",
                    issueLabel);
            PlanResult relaxedResult = runPlan(relaxed);
            allFailed = allFailed && relaxedResult.failed();
            for (RepositoryDTO repo : relaxedResult.repos()) {
                if (suggestions.size() >= MAX_SUGGESTIONS) break;
                tryAdd(repo, relaxed, suggestions, seen, excluded, ownerPrefix);
            }
        }

        if (suggestions.isEmpty() && allFailed) {
            throw new GitHubApiException(
                    "GitHub's search did not respond (it may be rate-limiting requests). Please try again in a minute.");
        }

        log.info("Open source suggestions for '{}': {} results from {} searches in {}ms (merged PRs: {})",
                username, suggestions.size(), plans.size(), System.currentTimeMillis() - startedAt, merged);

        List<String> shownBasis = basis.subList(0, Math.min(basis.size(), 6));
        return new RepoSuggestionSummaryDTO(new ArrayList<>(shownBasis), suggestions);
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    private SkillSummaryDTO awaitSkills(CompletableFuture<SkillSummaryDTO> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof RuntimeException runtime) {
                throw runtime; // keeps friendly errors such as "user not found" intact
            }
            throw new GitHubApiException("Could not analyse skills: " + cause.getMessage(), cause);
        }
    }

    private OpenSourceSummaryDTO loadPriorContributions(String username) {
        try {
            return contributionService.getContributions(username);
        } catch (Exception ex) {
            log.warn("Could not load prior contributions for '{}', using skills only: {}", username, ex.getMessage());
            return null;
        }
    }

    private List<RepositoryDTO> loadOwnRepositories(String username) {
        try {
            return gitHubService.getUserRepositories(username);
        } catch (Exception ex) {
            log.warn("Could not load repositories for '{}': {}", username, ex.getMessage());
            return List.of();
        }
    }

    /** Most recent distinct repos the user opened PRs on (repo name -> status of the newest PR). */
    private Map<String, String> recentContributedRepos(OpenSourceSummaryDTO prior) {
        Map<String, String> result = new LinkedHashMap<>();
        if (prior == null || prior.getContributions() == null) {
            return result;
        }
        for (OpenSourceContributionDTO c : prior.getContributions()) {
            if (c.getRepoName() == null) continue;
            if (result.size() >= MAX_PRIOR_REPOS && !result.containsKey(c.getRepoName())) break;
            result.putIfAbsent(c.getRepoName(), c.getStatus() == null ? "OPEN" : c.getStatus());
        }
        return result;
    }

    private RepositoryDTO fetchRepoDetails(String fullName) {
        try {
            return restTemplate.getForObject(baseUrl + "/repos/" + fullName, RepositoryDTO.class);
        } catch (RestClientException ex) {
            log.warn("Could not load details for repo '{}': {}", fullName, ex.getMessage());
            return null;
        }
    }

    private PlanResult runPlan(Plan plan) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", plan.query());
        params.put("sort", plan.sort());
        params.put("order", "desc");
        params.put("per_page", String.valueOf(PER_QUERY));
        URI url = UrlUtil.buildUri(baseUrl, "/search/repositories", params);
        try {
            GitHubSearchResponseDTO response = restTemplate.getForObject(url, GitHubSearchResponseDTO.class);
            List<RepositoryDTO> items = (response == null || response.getItems() == null)
                    ? List.of()
                    : response.getItems();
            log.info("Suggestion search [{}] '{}' -> {} results", plan.signal(), plan.query(), items.size());
            return new PlanResult(plan, items, false);
        } catch (Exception ex) {
            log.warn("Suggestion search failed for '{}': {}", plan.query(), ex.getMessage());
            return new PlanResult(plan, List.of(), true);
        }
    }

    private boolean tryAdd(RepositoryDTO repo, Plan plan, List<RepoSuggestionItemDTO> out,
                           Set<String> seen, Set<String> excluded, String ownerPrefix) {
        String key = repo.getFullName() != null ? repo.getFullName() : repo.getName();
        if (key == null) return false;
        String lower = key.toLowerCase();
        if (excluded.contains(lower) || lower.startsWith(ownerPrefix) || !seen.add(lower)) {
            return false;
        }
        out.add(new RepoSuggestionItemDTO(repo, buildReason(repo, plan)));
        return true;
    }

    private String firstUsefulTopic(List<String> topics, Set<String> languagesLower, Set<String> used) {
        if (topics == null) return null;
        for (String topic : topics) {
            if (isUsefulTopic(topic, languagesLower) && !used.contains(topic)) {
                return topic;
            }
        }
        return null;
    }

    private boolean isUsefulTopic(String topic, Set<String> languagesLower) {
        return topic != null
                && topic.length() >= 3
                && !NOISE_TOPICS.contains(topic)
                && !topic.startsWith("hacktoberfest")
                && !languagesLower.contains(topic);
    }

    private String quoteIfNeeded(String language) {
        return language.contains(" ") ? "\"" + language + "\"" : language;
    }

    private String buildReason(RepositoryDTO repo, Plan plan) {
        int stars = repo.getStars() == null ? 0 : repo.getStars();
        StringBuilder sb = new StringBuilder(plan.detail());
        sb.append(" It has ").append(formatCount(stars)).append(" stars");
        String fresh = freshness(repo.getUpdatedAt());
        if (fresh != null) {
            sb.append(", was updated ").append(fresh);
        }
        sb.append(", and has open issues labeled '").append(plan.issueLabel()).append("'.");
        return sb.toString();
    }

    private String freshness(String iso) {
        if (iso == null) return null;
        try {
            long days = Duration.between(Instant.parse(iso), Instant.now()).toDays();
            if (days <= 0) return "today";
            if (days == 1) return "yesterday";
            return days + " days ago";
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatCount(int count) {
        if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
