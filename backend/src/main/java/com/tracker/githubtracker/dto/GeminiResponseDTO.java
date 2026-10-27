package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponseDTO {

    private List<GeminiCandidateDTO> candidates;

    public List<GeminiCandidateDTO> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<GeminiCandidateDTO> candidates) {
        this.candidates = candidates;
    }
}
