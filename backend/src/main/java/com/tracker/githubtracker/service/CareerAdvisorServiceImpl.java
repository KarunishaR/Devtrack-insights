package com.tracker.githubtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.githubtracker.config.SimpleTtlCache;
import com.tracker.githubtracker.dto.*;
import com.tracker.githubtracker.exception.GeminiApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Uses Google's Gemini API to turn a detected skill set into:
 *  - a list of job roles the user is realistically eligible for
 *  - concrete suggestions on what to learn/improve next
 *
 * This is entirely optional - if gemini.api.key is left blank in
 * application.properties, this feature cleanly reports itself as
 * unavailable instead of crashing anything else in the app.
 *
 * RETRY BEHAVIOR: Gemini's free-tier infrastructure has documented,
 * fairly frequent "503 UNAVAILABLE - model overloaded" spikes that are
 * unrelated to your key or code - Google's own error message says to
 * simply retry. So instead of failing on the first 503, this service
 * automatically retries a few times with a short increasing delay
 * before giving up and reporting the failure.
 */
@Service
public class CareerAdvisorServiceImpl implements CareerAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(CareerAdvisorServiceImpl.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 800;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // The Gemini call is the slowest request in the app (several seconds), so
    // its result is cached per skill set. If the person's skills change, the
    // key changes and a fresh answer is generated.
    private final SimpleTtlCache<String, CareerSuggestionDTO> cache =
            new SimpleTtlCache<>(Duration.ofMinutes(30).toMillis());

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-flash-latest}")
    private String geminiModel;

    public CareerAdvisorServiceImpl(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        // Deliberately a plain RestTemplate (no GitHub-specific headers/interceptors).
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(40))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public CareerSuggestionDTO getCareerSuggestions(SkillSummaryDTO skillSummary) {
        if (geminiApiKey == null || geminiApiKey.isBlank()
                || skillSummary.getSkills() == null || skillSummary.getSkills().isEmpty()) {
            return generateSuggestions(skillSummary); // throws the appropriate friendly error
        }
        String cacheKey = skillSummary.getSkills().stream()
                .limit(10)
                .map(s -> s.getLanguage() + ":" + Math.round(s.getPercentage()))
                .collect(Collectors.joining("|"));
        return cache.getOrCompute(cacheKey, () -> generateSuggestions(skillSummary));
    }

    private CareerSuggestionDTO generateSuggestions(SkillSummaryDTO skillSummary) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new GeminiApiException(
                    "Career suggestions require a Gemini API key. Get a free key at "
                            + "https://aistudio.google.com/app/apikey and set gemini.api.key in application.properties.");
        }

        if (skillSummary.getSkills() == null || skillSummary.getSkills().isEmpty()) {
            throw new GeminiApiException(
                    "Not enough repository data to detect skills yet - this user has no repos with a detected language.");
        }

        String skillList = skillSummary.getSkills().stream()
                .map(s -> s.getLanguage() + " (" + Math.round(s.getPercentage()) + "% of their code, in "
                        + s.getRepoCount() + " repos)")
                .collect(Collectors.joining(", "));

        String prompt = """
                You are a career advisor for software developers.
                Based ONLY on these programming languages and technologies detected from a GitHub profile: %s

                IMPORTANT: Consider the FULL combination of languages/technologies listed above together, not
                just the single most frequent one. If the profile shows a mix such as JavaScript, HTML, and CSS
                together with a backend or database technology, treat that as full-stack / MERN-style web
                development experience and suggest roles accordingly, rather than suggesting only roles tied to
                whichever single language has the highest percentage.

                Respond with ONLY raw JSON, no markdown formatting, no code fences, no extra text,
                in exactly this shape:
                {
                  "eligibleRoles": [
                    {"title": "role name", "reason": "one sentence on why this skill combination fits, referencing specific technologies from the list"},
                    {"title": "role name", "reason": "..."}
                  ],
                  "improvementSuggestions": ["suggestion 1", "suggestion 2", "suggestion 3"]
                }

                Give 3 to 5 realistic entry/junior-level job roles that reflect the full combination of
                technologies (not only the top one), each with a short specific reason, and 3 to 5 concrete,
                actionable improvement suggestions.
                """.formatted(skillList);

        GeminiRequestDTO requestBody = new GeminiRequestDTO(
                List.of(new GeminiContentDTO(List.of(new GeminiPartDTO(prompt)))));

        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiModel, geminiApiKey);

        GeminiResponseDTO response = callWithRetry(url, requestBody);

        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new GeminiApiException("Gemini API returned an empty response.");
        }

        String rawText = response.getCandidates().get(0).getContent().getParts().get(0).getText();
        log.info("Gemini raw response: {}", rawText);

        // Gemini sometimes wraps JSON in ```json ... ``` even when told not to - strip that defensively.
        String cleaned = rawText.trim()
                .replaceAll("^```json", "")
                .replaceAll("^```", "")
                .replaceAll("```$", "")
                .trim();

        try {
            return objectMapper.readValue(cleaned, CareerSuggestionDTO.class);
        } catch (Exception ex) {
            log.error("Failed to parse Gemini response as JSON. Raw text was: {}", rawText, ex);
            throw new GeminiApiException("Gemini returned a response that could not be parsed. Check server logs for the raw output.");
        }
    }

    /**
     * Calls Gemini, automatically retrying on 503 (model overloaded) with
     * a short increasing delay. Any other error (401, 404, 429, etc.)
     * fails immediately since retrying won't help those.
     */
    private GeminiResponseDTO callWithRetry(String url, GeminiRequestDTO requestBody) {
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return restTemplate.postForObject(url, requestBody, GeminiResponseDTO.class);
            } catch (HttpServerErrorException ex) {
                HttpStatusCode status = ex.getStatusCode();
                boolean isOverloaded = status.value() == 503;

                if (!isOverloaded || attempt == MAX_ATTEMPTS) {
                    log.error("Gemini API call failed (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, ex.getMessage());
                    throw new GeminiApiException(
                            isOverloaded
                                    ? "Gemini's servers are currently overloaded and retries were unsuccessful. "
                                      + "This is a known, temporary issue on Google's side - please try again shortly."
                                    : "Gemini API returned a server error. Check server logs for detail.");
                }

                log.warn("Gemini API overloaded (503), retrying in {}ms (attempt {}/{})...",
                        backoffMs, attempt, MAX_ATTEMPTS);
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (RestClientException ex) {
                log.error("Gemini API call failed", ex);
                throw new GeminiApiException("Failed to reach the Gemini API. Check server logs for detail.");
            }
        }

        // Unreachable, but keeps the compiler happy.
        throw new GeminiApiException("Gemini API call failed after retries.");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
