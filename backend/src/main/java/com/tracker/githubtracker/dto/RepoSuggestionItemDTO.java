package com.tracker.githubtracker.dto;

/** A suggested repo paired with a specific, dynamically-generated reason it was picked. */
public class RepoSuggestionItemDTO {

    private RepositoryDTO repo;
    private String reason;

    public RepoSuggestionItemDTO() {}

    public RepoSuggestionItemDTO(RepositoryDTO repo, String reason) {
        this.repo = repo;
        this.reason = reason;
    }

    public RepositoryDTO getRepo() { return repo; }
    public void setRepo(RepositoryDTO repo) { this.repo = repo; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
