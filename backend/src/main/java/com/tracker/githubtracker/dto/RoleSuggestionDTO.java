package com.tracker.githubtracker.dto;

public class RoleSuggestionDTO {
    private String title;
    private String reason;

    public RoleSuggestionDTO() {}

    public RoleSuggestionDTO(String title, String reason) {
        this.title = title;
        this.reason = reason;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
