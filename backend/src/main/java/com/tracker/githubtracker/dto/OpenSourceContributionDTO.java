package com.tracker.githubtracker.dto;

/**
 * Clean, final shape for one real pull-request contribution, with its
 * actual current status - this is what makes the feature "real" rather
 * than guessed: status comes directly from GitHub (merged_at present =
 * MERGED, state=closed with no merge = CLOSED, otherwise OPEN).
 */
public class OpenSourceContributionDTO {

    private String repoName;
    private String repoUrl;
    private String prTitle;
    private String prUrl;
    private String status; // OPEN, MERGED, CLOSED
    private String summary;
    private String createdAt;
    private String updatedAt;

    public OpenSourceContributionDTO() {}

    public OpenSourceContributionDTO(String repoName, String repoUrl, String prTitle, String prUrl,
                                      String status, String summary, String createdAt, String updatedAt) {
        this.repoName = repoName;
        this.repoUrl = repoUrl;
        this.prTitle = prTitle;
        this.prUrl = prUrl;
        this.status = status;
        this.summary = summary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getPrTitle() { return prTitle; }
    public void setPrTitle(String prTitle) { this.prTitle = prTitle; }
    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
