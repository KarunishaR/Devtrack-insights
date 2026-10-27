package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiContentDTO {

    private List<GeminiPartDTO> parts;

    public GeminiContentDTO() {
    }

    public GeminiContentDTO(List<GeminiPartDTO> parts) {
        this.parts = parts;
    }

    public List<GeminiPartDTO> getParts() {
        return parts;
    }

    public void setParts(List<GeminiPartDTO> parts) {
        this.parts = parts;
    }
}
