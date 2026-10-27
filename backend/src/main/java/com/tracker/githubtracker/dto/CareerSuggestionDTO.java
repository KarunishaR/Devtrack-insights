package com.tracker.githubtracker.dto;

import java.util.List;

/** Final clean shape returned by our /career-suggestions endpoint. */
public class CareerSuggestionDTO {

    private List<RoleSuggestionDTO> eligibleRoles;
    private List<String> improvementSuggestions;

    public CareerSuggestionDTO() {
    }

    public CareerSuggestionDTO(List<RoleSuggestionDTO> eligibleRoles, List<String> improvementSuggestions) {
        this.eligibleRoles = eligibleRoles;
        this.improvementSuggestions = improvementSuggestions;
    }

    public List<RoleSuggestionDTO> getEligibleRoles() {
        return eligibleRoles;
    }

    public void setEligibleRoles(List<RoleSuggestionDTO> eligibleRoles) {
        this.eligibleRoles = eligibleRoles;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(List<String> improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }
}
