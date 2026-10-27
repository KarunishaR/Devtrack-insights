package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubEventRepoDTO {
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
