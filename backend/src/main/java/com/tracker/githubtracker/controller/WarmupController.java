package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.service.ActivityService;
import com.tracker.githubtracker.service.CareerAdvisorService;
import com.tracker.githubtracker.service.ContributionService;
import com.tracker.githubtracker.service.OpenSourceContributionService;
import com.tracker.githubtracker.service.OpenSourceSuggestionService;
import com.tracker.githubtracker.service.SkillAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Starts the expensive work for a user in the BACKGROUND and returns at once.
 *
 * The landing page calls this the moment "Analyze" is pressed. By the time
 * the browser has finished navigating and loading the next page, skills,
 * pull requests, contributions, activity, suggestions and the AI career
 * advice are already being computed - so those pages find their data ready
 * (or nearly ready) instead of starting from zero.
 *
 * Results land in the same caches the normal endpoints use, and the
 * single-flight cache means that if a page asks for something that is still
 * being computed, it simply waits for that one computation rather than
 * starting a second one.
 */
@RestController
@RequestMapping("/api/github")
public class WarmupController {

    private static final Logger log = LoggerFactory.getLogger(WarmupController.class);

    private final SkillAnalysisService skillAnalysisService;
    private final ContributionService contributionService;
    private final ActivityService activityService;
    private final OpenSourceContributionService openSourceContributionService;
    private final OpenSourceSuggestionService suggestionService;
    private final CareerAdvisorService careerAdvisorService;
    private final ExecutorService orchestrator;

    public WarmupController(SkillAnalysisService skillAnalysisService,
                            ContributionService contributionService,
                            ActivityService activityService,
                            OpenSourceContributionService openSourceContributionService,
                            OpenSourceSuggestionService suggestionService,
                            CareerAdvisorService careerAdvisorService,
                            @Qualifier("orchestratorExecutor") ExecutorService orchestrator) {
        this.skillAnalysisService = skillAnalysisService;
        this.contributionService = contributionService;
        this.activityService = activityService;
        this.openSourceContributionService = openSourceContributionService;
        this.suggestionService = suggestionService;
        this.careerAdvisorService = careerAdvisorService;
        this.orchestrator = orchestrator;
    }

    /** GET http://localhost:8080/api/github/{username}/warmup */
    @GetMapping("/{username}/warmup")
    public ResponseEntity<Map<String, String>> warmup(@PathVariable String username) {
        final int year = Year.now().getValue();

        runInBackground(username, "skills", () -> skillAnalysisService.getSkillSummary(username));
        runInBackground(username, "contributions", () -> contributionService.getContributionSummary(username, year));
        runInBackground(username, "activity", () -> activityService.getRecentActivity(username));
        runInBackground(username, "open-source", () -> openSourceContributionService.getContributions(username));
        runInBackground(username, "suggestions", () -> suggestionService.suggestForUser(username));
        runInBackground(username, "career",
                () -> careerAdvisorService.getCareerSuggestions(skillAnalysisService.getSkillSummary(username)));

        return ResponseEntity.accepted().body(Map.of("status", "warming"));
    }

    private void runInBackground(String username, String name, Runnable task) {
        orchestrator.submit(() -> {
            try {
                task.run();
            } catch (Exception ex) {
                // Warm-up is best-effort: an unknown user or a missing API key is not an error here.
                log.debug("Warm-up '{}' for '{}' skipped: {}", name, username, ex.getMessage());
            }
        });
    }
}
