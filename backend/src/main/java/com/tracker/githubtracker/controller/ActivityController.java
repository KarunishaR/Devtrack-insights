package com.tracker.githubtracker.controller;

import com.tracker.githubtracker.dto.ActivityItemDTO;
import com.tracker.githubtracker.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/github")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Recent, itemized activity feed (last ~90 days) - "what did they
     * actually do", not just a contribution count.
     * GET http://localhost:8080/api/github/{username}/activity
     */
    @GetMapping("/{username}/activity")
    public ResponseEntity<List<ActivityItemDTO>> getActivity(@PathVariable String username) {
        return ResponseEntity.ok(activityService.getRecentActivity(username));
    }
}
