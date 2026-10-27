package com.tracker.githubtracker.dto;

/** Clean, human-readable shape for a single activity feed entry. */
public class ActivityItemDTO {
    private String date;
    private String timestamp;
    private String type;
    private String repoName;
    private String description;

    public ActivityItemDTO() {}

    public ActivityItemDTO(String date, String timestamp, String type, String repoName, String description) {
        this.date = date;
        this.timestamp = timestamp;
        this.type = type;
        this.repoName = repoName;
        this.description = description;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
