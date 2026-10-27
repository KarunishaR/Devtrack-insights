package com.tracker.githubtracker.dto;

import java.util.List;

/** Request body shape expected by Google's Generative Language API (generateContent). */
public class GeminiRequestDTO {

    private List<GeminiContentDTO> contents;

    public GeminiRequestDTO() {
    }

    public GeminiRequestDTO(List<GeminiContentDTO> contents) {
        this.contents = contents;
    }

    public List<GeminiContentDTO> getContents() {
        return contents;
    }

    public void setContents(List<GeminiContentDTO> contents) {
        this.contents = contents;
    }
}
