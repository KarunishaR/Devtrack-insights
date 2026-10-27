package com.tracker.githubtracker.dto;

public class SkillDTO {

    private String language;
    private int repoCount;
    private double percentage;

    public SkillDTO() {
    }

    public SkillDTO(String language, int repoCount, double percentage) {
        this.language = language;
        this.repoCount = repoCount;
        this.percentage = percentage;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getRepoCount() {
        return repoCount;
    }

    public void setRepoCount(int repoCount) {
        this.repoCount = repoCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
