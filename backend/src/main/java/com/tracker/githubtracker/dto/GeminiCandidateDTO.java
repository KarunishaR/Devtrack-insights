package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiCandidateDTO {

    private GeminiContentDTO content;

    public GeminiContentDTO getContent() {
        return content;
    }

    public void setContent(GeminiContentDTO content) {
        this.content = content;
    }
}
