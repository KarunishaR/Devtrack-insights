package com.tracker.githubtracker.dto;

/** Daily counts rolled up into a single month, e.g. "2026-07" -> 143 contributions. */
public class MonthlyContributionDTO {

    private String month;
    private int totalContributions;

    public MonthlyContributionDTO() {
    }

    public MonthlyContributionDTO(String month, int totalContributions) {
        this.month = month;
        this.totalContributions = totalContributions;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getTotalContributions() {
        return totalContributions;
    }

    public void setTotalContributions(int totalContributions) {
        this.totalContributions = totalContributions;
    }
}
