package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContributionCalendarRawDTO {

    private int totalContributions;
    private List<ContributionWeekDTO> weeks;

    public int getTotalContributions() {
        return totalContributions;
    }

    public void setTotalContributions(int totalContributions) {
        this.totalContributions = totalContributions;
    }

    public List<ContributionWeekDTO> getWeeks() {
        return weeks;
    }

    public void setWeeks(List<ContributionWeekDTO> weeks) {
        this.weeks = weeks;
    }
}
