package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Shape of GitHub's /search/repositories and /search/issues response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubSearchResponseDTO {

    @JsonAlias("total_count")
    private Integer totalCount;

    private List<RepositoryDTO> items;

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public List<RepositoryDTO> getItems() {
        return items;
    }

    public void setItems(List<RepositoryDTO> items) {
        this.items = items;
    }
}
