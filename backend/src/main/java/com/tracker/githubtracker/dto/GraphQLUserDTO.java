package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLUserDTO {

    private ContributionsCollectionDTO contributionsCollection;

    public ContributionsCollectionDTO getContributionsCollection() {
        return contributionsCollection;
    }

    public void setContributionsCollection(ContributionsCollectionDTO contributionsCollection) {
        this.contributionsCollection = contributionsCollection;
    }
}
