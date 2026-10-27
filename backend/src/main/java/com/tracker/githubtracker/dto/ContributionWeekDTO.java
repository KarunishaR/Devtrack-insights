package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContributionWeekDTO {

    private List<ContributionDayDTO> contributionDays;

    public List<ContributionDayDTO> getContributionDays() {
        return contributionDays;
    }

    public void setContributionDays(List<ContributionDayDTO> contributionDays) {
        this.contributionDays = contributionDays;
    }
}
