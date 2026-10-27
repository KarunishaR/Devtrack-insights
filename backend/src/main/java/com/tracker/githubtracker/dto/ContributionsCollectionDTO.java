package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContributionsCollectionDTO {

    private ContributionCalendarRawDTO contributionCalendar;

    public ContributionCalendarRawDTO getContributionCalendar() {
        return contributionCalendar;
    }

    public void setContributionCalendar(ContributionCalendarRawDTO contributionCalendar) {
        this.contributionCalendar = contributionCalendar;
    }
}
