package com.tracker.githubtracker.dto;

import java.util.List;

/**
 * Combined "everything at once" shape for the dashboard endpoint -
 * what the website shows the moment a user opens it.
 *
 * If an individual section fails (e.g. Gemini key not configured),
 * that section is simply left null and a note explaining why is added
 * to "notes", rather than failing the whole dashboard.
 */
public class DashboardDTO {

    private GitHubProfileDTO profile;
    private SkillSummaryDTO skills;
    private ContributionSummaryDTO contributions;
    private CareerSuggestionDTO careerSuggestions;
    private RepoSuggestionSummaryDTO repoSuggestions;
    private List<String> notes;

    public GitHubProfileDTO getProfile() {
        return profile;
    }

    public void setProfile(GitHubProfileDTO profile) {
        this.profile = profile;
    }

    public SkillSummaryDTO getSkills() {
        return skills;
    }

    public void setSkills(SkillSummaryDTO skills) {
        this.skills = skills;
    }

    public ContributionSummaryDTO getContributions() {
        return contributions;
    }

    public void setContributions(ContributionSummaryDTO contributions) {
        this.contributions = contributions;
    }

    public CareerSuggestionDTO getCareerSuggestions() {
        return careerSuggestions;
    }

    public void setCareerSuggestions(CareerSuggestionDTO careerSuggestions) {
        this.careerSuggestions = careerSuggestions;
    }

    public RepoSuggestionSummaryDTO getRepoSuggestions() {
        return repoSuggestions;
    }

    public void setRepoSuggestions(RepoSuggestionSummaryDTO repoSuggestions) {
        this.repoSuggestions = repoSuggestions;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }
}
