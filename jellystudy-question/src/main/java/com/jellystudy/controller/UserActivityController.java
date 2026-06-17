package com.jellystudy.controller;

import com.jellystudy.dubbo.UserActivityDubboService;
import com.jellystudy.entity.UserActivity;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class UserActivityController {

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.UserActivityDubboService", timeout = 30000, check = false)
    private UserActivityDubboService activityService;

    @PostMapping
    public Map<String, Object> recordActivity(@RequestBody UserActivity activity) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserActivity saved = activityService.recordActivity(activity);
            result.put("success", true);
            result.put("activity", saved);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/user/{userId}")
    public List<UserActivity> getUserActivities(@PathVariable Long userId) {
        return activityService.findByUserId(userId);
    }

    @GetMapping("/user/{userId}/type/{type}")
    public List<UserActivity> getUserActivitiesByType(@PathVariable Long userId, @PathVariable String type) {
        return activityService.findByUserIdAndType(userId, type);
    }

    @GetMapping("/user/{userId}/stats")
    public Map<String, Object> getUserActivityStats(@PathVariable Long userId) {
        return activityService.getUserActivityStats(userId);
    }

    @GetMapping("/user/{userId}/recent")
    public List<UserActivity> getRecentActivities(@PathVariable Long userId, @RequestParam(defaultValue = "20") int limit) {
        return activityService.findRecentActivities(userId, limit);
    }

    @GetMapping("/user/{userId}/ai-analysis")
    public Map<String, Object> getAIBehaviorAnalysis(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String analysis = activityService.getAIBehaviorAnalysis(userId);
            result.put("success", true);
            result.put("analysis", analysis);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
