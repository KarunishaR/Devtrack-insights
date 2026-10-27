package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps the JSON returned by GET https://api.github.com/users/{username}
 * AND is returned directly as our own API's response.
 *
 * IMPORTANT: fields that GitHub names differently from our clean
 * camelCase (avatar_url, html_url, etc.) use @JsonAlias, NOT
 * @JsonProperty. @JsonAlias only affects READING GitHub's snake_case
 * JSON; our own OUTPUT still serializes using the plain camelCase field
 * name (avatarUrl, htmlUrl, ...), which is what the frontend expects.
 * Using @JsonProperty here would have renamed our own API's output to
 * snake_case too, silently breaking the frontend.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubProfileDTO {

    private String login;
    private String name;

    @JsonAlias("avatar_url")
    private String avatarUrl;

    private String bio;
    private String company;
    private String location;
    private String blog;
    private String email;

    @JsonAlias("html_url")
    private String htmlUrl;

    private Integer followers;
    private Integer following;

    @JsonAlias("public_repos")
    private Integer publicRepos;

    @JsonAlias("public_gists")
    private Integer publicGists;

    @JsonAlias("created_at")
    private String createdAt;

    public GitHubProfileDTO() {
    }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getBlog() { return blog; }
    public void setBlog(String blog) { this.blog = blog; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public Integer getFollowers() { return followers; }
    public void setFollowers(Integer followers) { this.followers = followers; }
    public Integer getFollowing() { return following; }
    public void setFollowing(Integer following) { this.following = following; }
    public Integer getPublicRepos() { return publicRepos; }
    public void setPublicRepos(Integer publicRepos) { this.publicRepos = publicRepos; }
    public Integer getPublicGists() { return publicGists; }
    public void setPublicGists(Integer publicGists) { this.publicGists = publicGists; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
