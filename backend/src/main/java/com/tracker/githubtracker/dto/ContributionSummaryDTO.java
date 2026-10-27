package com.tracker.githubtracker.dto;

import java.util.List;

/** Final clean shape returned by our /contributions endpoint - day-wise AND month-wise. */
public class ContributionSummaryDTO {

    private int totalContributions;
    private List<ContributionDayDTO> daily;
    private List<MonthlyContributionDTO> monthly;

    public ContributionSummaryDTO() {
    }

    public ContributionSummaryDTO(int totalContributions, List<ContributionDayDTO> daily, List<MonthlyContributionDTO> monthly) {
        this.totalContributions = totalContributions;
        this.daily = daily;
        this.monthly = monthly;
    }

    public int getTotalContributions() {
        return totalContributions;
    }

    public void setTotalContributions(int totalContributions) {
        this.totalContributions = totalContributions;
    }

    public List<ContributionDayDTO> getDaily() {
        return daily;
    }

    public void setDaily(List<ContributionDayDTO> daily) {
        this.daily = daily;
    }

    public List<MonthlyContributionDTO> getMonthly() {
        return monthly;
    }

    public void setMonthly(List<MonthlyContributionDTO> monthly) {
        this.monthly = monthly;
    }
}
