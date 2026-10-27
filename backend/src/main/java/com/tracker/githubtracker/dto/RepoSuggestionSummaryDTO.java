package com.tracker.githubtracker.dto;

import java.util.List;

/** Final clean shape returned by our /repo-suggestions endpoint. */
public class RepoSuggestionSummaryDTO {

    private List<String> basedOnSkills;
    private List<RepoSuggestionItemDTO> suggestions;

    public RepoSuggestionSummaryDTO() {
    }

    public RepoSuggestionSummaryDTO(List<String> basedOnSkills, List<RepoSuggestionItemDTO> suggestions) {
        this.basedOnSkills = basedOnSkills;
        this.suggestions = suggestions;
    }

    public List<String> getBasedOnSkills() { return basedOnSkills; }
    public void setBasedOnSkills(List<String> basedOnSkills) { this.basedOnSkills = basedOnSkills; }
    public List<RepoSuggestionItemDTO> getSuggestions() { return suggestions; }
    public void setSuggestions(List<RepoSuggestionItemDTO> suggestions) { this.suggestions = suggestions; }
}
