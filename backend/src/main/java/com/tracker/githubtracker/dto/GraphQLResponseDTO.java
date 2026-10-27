package com.tracker.githubtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Top-level shape of every GitHub GraphQL response.
 * GraphQL always returns HTTP 200, even on failure - actual errors
 * show up in the "errors" array instead of an HTTP status code, so we
 * always check this field explicitly in the service layer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLResponseDTO {

    private GraphQLDataDTO data;
    private List<Map<String, Object>> errors;

    public GraphQLDataDTO getData() {
        return data;
    }

    public void setData(GraphQLDataDTO data) {
        this.data = data;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

    public void setErrors(List<Map<String, Object>> errors) {
        this.errors = errors;
    }
}
