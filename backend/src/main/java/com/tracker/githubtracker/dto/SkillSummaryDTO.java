package com.tracker.githubtracker.dto;

import java.util.List;

public class SkillSummaryDTO {

    private String topLanguage;
    private int totalReposAnalyzed;
    private List<SkillDTO> skills;

    public SkillSummaryDTO() {
    }

    public SkillSummaryDTO(String topLanguage, int totalReposAnalyzed, List<SkillDTO> skills) {
        this.topLanguage = topLanguage;
        this.totalReposAnalyzed = totalReposAnalyzed;
        this.skills = skills;
    }

    public String getTopLanguage() {
        return topLanguage;
    }

    public void setTopLanguage(String topLanguage) {
        this.topLanguage = topLanguage;
    }

    public int getTotalReposAnalyzed() {
        return totalReposAnalyzed;
    }

    public void setTotalReposAnalyzed(int totalReposAnalyzed) {
        this.totalReposAnalyzed = totalReposAnalyzed;
    }

    public List<SkillDTO> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillDTO> skills) {
        this.skills = skills;
    }
}
