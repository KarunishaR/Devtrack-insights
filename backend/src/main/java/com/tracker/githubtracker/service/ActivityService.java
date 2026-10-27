package com.tracker.githubtracker.service;

import com.tracker.githubtracker.dto.ActivityItemDTO;

import java.util.List;

public interface ActivityService {
    List<ActivityItemDTO> getRecentActivity(String username);
}
