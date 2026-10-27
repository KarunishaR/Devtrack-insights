package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/** Maps one item from GET /users/{username}/events/public */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubEventDTO {
    private String id;
    private String type;
    private GitHubEventRepoDTO repo;

    @JsonProperty("created_at")
    private String createdAt;

    private Map<String, Object> payload;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public GitHubEventRepoDTO getRepo() { return repo; }
    public void setRepo(GitHubEventRepoDTO repo) { this.repo = repo; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
