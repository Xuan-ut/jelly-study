package com.jellystudy.dubbo;

import com.jellystudy.entity.UserActivity;

import java.util.List;
import java.util.Map;

public interface UserActivityDubboService {
    UserActivity recordActivity(UserActivity activity);
    List<UserActivity> findByUserId(Long userId);
    List<UserActivity> findByUserIdAndType(Long userId, String activityType);
    Map<String, Object> getUserActivityStats(Long userId);
    List<UserActivity> findRecentActivities(Long userId, int limit);
    String getAIBehaviorAnalysis(Long userId);
}
