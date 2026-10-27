package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequestInfoDTO {

    @JsonAlias("merged_at")
    private String mergedAt;

    public String getMergedAt() { return mergedAt; }
    public void setMergedAt(String mergedAt) { this.mergedAt = mergedAt; }
}
