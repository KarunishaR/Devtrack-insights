package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Maps GitHub repo JSON (from /repos and /search/repositories) AND is
 * returned directly as part of our own API's response.
 *
 * Uses @JsonAlias (read-only remap) instead of @JsonProperty for the
 * same reason as GitHubProfileDTO - see that file's comment. Using
 * @JsonProperty here previously caused our own output to silently use
 * GitHub's snake_case names (stargazers_count, html_url, ...) instead
 * of the camelCase the frontend expects (stars, htmlUrl, ...), making
 * stars/forks/links always read as 0 / broken on the frontend.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryDTO {

    private String name;

    @JsonAlias("full_name")
    private String fullName;

    private String description;

    @JsonAlias("html_url")
    private String htmlUrl;

    private String language;

    @JsonAlias("stargazers_count")
    private Integer stars;

    @JsonAlias("forks_count")
    private Integer forks;

    @JsonAlias("open_issues_count")
    private Integer openIssues;

    private Boolean fork;

    private List<String> topics;

    @JsonAlias("updated_at")
    private String updatedAt;

    public RepositoryDTO() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public Integer getForks() { return forks; }
    public void setForks(Integer forks) { this.forks = forks; }
    public Integer getOpenIssues() { return openIssues; }
    public void setOpenIssues(Integer openIssues) { this.openIssues = openIssues; }
    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }
    public Boolean getFork() { return fork; }
    public void setFork(Boolean fork) { this.fork = fork; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
