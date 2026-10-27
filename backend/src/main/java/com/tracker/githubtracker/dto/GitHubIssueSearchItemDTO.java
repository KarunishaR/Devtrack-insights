package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Maps one item from GET /search/issues (used here to find PRs authored by a user). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubIssueSearchItemDTO {

    private String title;
    private String state;
    private String body;

    @JsonAlias("html_url")
    private String htmlUrl;

    @JsonAlias("repository_url")
    private String repositoryUrl;

    @JsonAlias("created_at")
    private String createdAt;

    @JsonAlias("updated_at")
    private String updatedAt;

    @JsonAlias("pull_request")
    private GitHubPullRequestInfoDTO pullRequest;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public GitHubPullRequestInfoDTO getPullRequest() { return pullRequest; }
    public void setPullRequest(GitHubPullRequestInfoDTO pullRequest) { this.pullRequest = pullRequest; }
}
