package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.GitHubProfileDTO;
import com.tracker.githubtracker.dto.RepositoryDTO;

import java.util.List;

public interface GitHubService {

    GitHubProfileDTO getUserProfile(String username);

    List<RepositoryDTO> getUserRepositories(String username);
}
