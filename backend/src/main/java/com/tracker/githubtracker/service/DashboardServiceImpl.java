package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.DashboardDTO;
import com.tracker.githubtracker.dto.SkillSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles everything the frontend needs in one call: profile, skills,
 * contribution calendar, career suggestions, and repo recommendations.
 *
 * Each section is fetched independently. If one section fails (e.g. no
 * Gemini key configured, or GitHub token missing for contributions),
 * that section is simply left out and a plain-English note is added -
 * the rest of the dashboard still loads normally.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final GitHubService gitHubService;
    private final SkillAnalysisService skillAnalysisService;
    private final ContributionService contributionService;
    private final CareerAdvisorService careerAdvisorService;
    private final RepoRecommendationService repoRecommendationService;

    public DashboardServiceImpl(GitHubService gitHubService,
                                 SkillAnalysisService skillAnalysisService,
                                 ContributionService contributionService,
                                 CareerAdvisorService careerAdvisorService,
                                 RepoRecommendationService repoRecommendationService) {
        this.gitHubService = gitHubService;
        this.skillAnalysisService = skillAnalysisService;
        this.contributionService = contributionService;
        this.careerAdvisorService = careerAdvisorService;
        this.repoRecommendationService = repoRecommendationService;
    }

    @Override
    public DashboardDTO getDashboard(String username) {
        DashboardDTO dashboard = new DashboardDTO();
        List<String> notes = new ArrayList<>();

        // Profile is the one section that MUST succeed - if the username
        // doesn't exist, the whole dashboard should fail with a clear 404,
        // not silently show an empty page.
        dashboard.setProfile(gitHubService.getUserProfile(username));

        SkillSummaryDTO skills = null;
        try {
            skills = skillAnalysisService.getSkillSummary(username);
            dashboard.setSkills(skills);
        } catch (Exception ex) {
            log.warn("Skill analysis failed for '{}': {}", username, ex.getMessage());
            notes.add("Skill analysis unavailable: " + ex.getMessage());
        }

        try {
            dashboard.setContributions(contributionService.getContributionSummary(username, null));
        } catch (Exception ex) {
            log.warn("Contribution fetch failed for '{}': {}", username, ex.getMessage());
            notes.add("Contribution calendar unavailable: " + ex.getMessage());
        }

        if (skills != null) {
            try {
                dashboard.setCareerSuggestions(careerAdvisorService.getCareerSuggestions(skills));
            } catch (Exception ex) {
                log.warn("Career suggestions failed for '{}': {}", username, ex.getMessage());
                notes.add("Career suggestions unavailable: " + ex.getMessage());
            }

            try {
                dashboard.setRepoSuggestions(repoRecommendationService.suggestRepositories(skills));
            } catch (Exception ex) {
                log.warn("Repo suggestions failed for '{}': {}", username, ex.getMessage());
                notes.add("Repo suggestions unavailable: " + ex.getMessage());
            }
        }

        dashboard.setNotes(notes);
        return dashboard;
    }
}
