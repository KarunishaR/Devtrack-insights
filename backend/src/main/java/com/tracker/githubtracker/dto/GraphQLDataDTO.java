package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLDataDTO {

    private GraphQLUserDTO user;

    public GraphQLUserDTO getUser() {
        return user;
    }

    public void setUser(GraphQLUserDTO user) {
        this.user = user;
    }
}
