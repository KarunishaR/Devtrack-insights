package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One square on the GitHub contribution calendar: a single day + its
 * count. Maps GraphQL's "contributionCount" field AND is returned
 * directly in our own /contributions API response.
 *
 * Uses @JsonAlias (read-only) rather than @JsonProperty - this was the
 * actual root cause of the contribution calendar always showing 0:
 * @JsonProperty("contributionCount") had also forced our OWN API output
 * to use "contributionCount" as the JSON key, but the frontend reads
 * "count" - so every day silently deserialized as undefined -> 0.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContributionDayDTO {

    private String date;

    @JsonAlias("contributionCount")
    private Integer count;

    public ContributionDayDTO() {
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
