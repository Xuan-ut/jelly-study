package com.jellystudy.service;

import com.jellystudy.dubbo.AIDubboService;
import com.jellystudy.dubbo.UserActivityDubboService;
import com.jellystudy.entity.UserActivity;
import com.jellystudy.repository.mongo.UserActivityRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.UserActivityDubboService")
public class UserActivityServiceImpl implements UserActivityDubboService {

    @Autowired
    private UserActivityRepository activityRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.AIDubboService", timeout = 120000, check = false)
    private AIDubboService aiService;

    private static final String ACTIVITY_CACHE_PREFIX = "activity:user:";
    private static final String STATS_CACHE_PREFIX = "activity:stats:";

    @Override
    public UserActivity recordActivity(UserActivity activity) {
        if (activity.getCreateTime() == null) {
            activity.setCreateTime(new Date());
        }
        UserActivity saved = activityRepository.save(activity);
        clearUserCache(activity.getUserId());
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("activityId", saved.getId());
            message.put("userId", saved.getUserId());
            message.put("activityType", saved.getActivityType());
            message.put("targetId", saved.getTargetId());
            message.put("createTime", saved.getCreateTime().toString());
            rabbitTemplate.convertAndSend("jellystudy.activity.exchange", "activity.record", message);
            log.info("用户行为事件已发送到MQ: userId={}, type={}", saved.getUserId(), saved.getActivityType());
        } catch (Exception e) {
            log.warn("发送行为事件到MQ失败: {}", e.getMessage());
        }
        log.info("记录用户行为: userId={}, type={}, target={}", activity.getUserId(), activity.getActivityType(), activity.getTargetId());
        return saved;
    }

    @Override
    public List<UserActivity> findByUserId(Long userId) {
        String cacheKey = ACTIVITY_CACHE_PREFIX + userId + ":all";
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<UserActivity>) cached;
            }
        } catch (Exception e) {
            log.warn("读取活动缓存失败: {}", e.getMessage());
        }
        List<UserActivity> activities = activityRepository.findByUserIdOrderByCreateTimeDesc(userId);
        try {
            redisTemplate.opsForValue().set(cacheKey, activities, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入活动缓存失败: {}", e.getMessage());
        }
        return activities;
    }

    @Override
    public List<UserActivity> findByUserIdAndType(Long userId, String activityType) {
        return activityRepository.findByUserIdAndActivityTypeOrderByCreateTimeDesc(userId, activityType);
    }

    @Override
    public Map<String, Object> getUserActivityStats(Long userId) {
        String cacheKey = STATS_CACHE_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Map) {
                return (Map<String, Object>) cached;
            }
        } catch (Exception e) {
            log.warn("读取统计缓存失败: {}", e.getMessage());
        }

        Map<String, Object> stats = new HashMap<>();
        long total = activityRepository.countByUserId(userId);
        stats.put("totalActivities", total);
        stats.put("questionCount", activityRepository.countByUserIdAndActivityType(userId, UserActivity.TYPE_QUESTION));
        stats.put("answerCount", activityRepository.countByUserIdAndActivityType(userId, UserActivity.TYPE_ANSWER));
        stats.put("commentCount", activityRepository.countByUserIdAndActivityType(userId, UserActivity.TYPE_COMMENT));
        stats.put("likeCount", activityRepository.countByUserIdAndActivityType(userId, UserActivity.TYPE_LIKE));
        stats.put("browseCount", activityRepository.countByUserIdAndActivityType(userId, UserActivity.TYPE_BROWSE));
        stats.put("studyCount", activityRepository.countByUserIdAndActivityType(userId, UserActivity.TYPE_STUDY));

        List<UserActivity> recent = activityRepository.findByUserIdOrderByCreateTimeDesc(userId);
        if (!recent.isEmpty()) {
            stats.put("lastActivityTime", recent.get(0).getCreateTime());
        }

        Map<String, Long> typeDistribution = new HashMap<>();
        String[] types = {UserActivity.TYPE_QUESTION, UserActivity.TYPE_ANSWER, UserActivity.TYPE_COMMENT, UserActivity.TYPE_LIKE, UserActivity.TYPE_BROWSE, UserActivity.TYPE_STUDY};
        for (String type : types) {
            long count = activityRepository.countByUserIdAndActivityType(userId, type);
            if (count > 0) typeDistribution.put(type, count);
        }
        stats.put("typeDistribution", typeDistribution);

        try {
            redisTemplate.opsForValue().set(cacheKey, stats, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入统计缓存失败: {}", e.getMessage());
        }
        return stats;
    }

    @Override
    public List<UserActivity> findRecentActivities(Long userId, int limit) {
        List<UserActivity> all = activityRepository.findByUserIdOrderByCreateTimeDesc(userId);
        if (all.size() <= limit) return all;
        return new ArrayList<>(all.subList(0, limit));
    }

    @Override
    public String getAIBehaviorAnalysis(Long userId) {
        List<UserActivity> activities = findRecentActivities(userId, 50);
        if (activities.isEmpty()) {
            return "暂无行为数据可供分析。";
        }
        List<Map<String, Object>> activityData = activities.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("type", a.getActivityType());
            map.put("target", a.getTargetTitle());
            map.put("content", a.getContent());
            map.put("time", a.getCreateTime().toString());
            return map;
        }).collect(Collectors.toList());
        try {
            return aiService.analyzeUserBehavior(userId, activityData);
        } catch (Exception e) {
            log.error("AI行为分析失败: {}", e.getMessage());
            return "AI分析暂时不可用，请稍后再试。";
        }
    }

    private void clearUserCache(Long userId) {
        try {
            redisTemplate.delete(ACTIVITY_CACHE_PREFIX + userId + ":all");
            redisTemplate.delete(STATS_CACHE_PREFIX + userId);
        } catch (Exception e) {
            log.warn("清除缓存失败: {}", e.getMessage());
        }
    }
}
