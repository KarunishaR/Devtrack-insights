package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubIssueSearchResponseDTO {

    @JsonAlias("total_count")
    private Integer totalCount;

    private List<GitHubIssueSearchItemDTO> items;

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public List<GitHubIssueSearchItemDTO> getItems() { return items; }
    public void setItems(List<GitHubIssueSearchItemDTO> items) { this.items = items; }
}
