package com.jellystudy.service;

import com.jellystudy.dubbo.StudyProgressDubboService;
import com.jellystudy.entity.StudyProgress;
import com.jellystudy.repository.mongo.StudyProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.StudyProgressDubboService")
public class StudyProgressServiceImpl implements StudyProgressDubboService {

    @Autowired
    private StudyProgressRepository progressRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String PROGRESS_CACHE_PREFIX = "progress:user:kp:";
    private static final String STATS_CACHE_PREFIX = "progress:stats:";

    @Override
    public StudyProgress create(StudyProgress progress) {
        progress.setCreateTime(new Date());
        progress.setUpdateTime(new Date());
        if (progress.getStatus() == null) {
            progress.setStatus("NOT_STARTED");
        }
        StudyProgress saved = progressRepository.save(progress);
        cacheProgress(saved);
        invalidateStatsCache(progress.getUserId());
        return saved;
    }

    @Override
    public StudyProgress update(StudyProgress progress) {
        progress.setUpdateTime(new Date());
        if (progress.getProgress() >= 100) {
            progress.setStatus("COMPLETED");
        } else if (progress.getProgress() > 0) {
            progress.setStatus("IN_PROGRESS");
        }
        StudyProgress saved = progressRepository.save(progress);
        cacheProgress(saved);
        invalidateStatsCache(progress.getUserId());

        if (progress.getProgress() >= 100) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("userId", progress.getUserId());
            eventData.put("knowledgePointId", progress.getKnowledgePointId());
            eventData.put("knowledgePointName", progress.getKnowledgePointName());
            rabbitTemplate.convertAndSend("study.event.exchange", "knowledge.completed", eventData);
            log.info("知识点学习完成: userId={}, kp={}", progress.getUserId(), progress.getKnowledgePointName());
        }

        return saved;
    }

    @Override
    public StudyProgress findByUserAndKnowledgePoint(Long userId, String knowledgePointId) {
        String cacheKey = PROGRESS_CACHE_PREFIX + userId + ":" + knowledgePointId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof StudyProgress) {
            return (StudyProgress) cached;
        }
        StudyProgress progress = progressRepository.findByUserIdAndKnowledgePointId(userId, knowledgePointId).orElse(null);
        if (progress != null) {
            cacheProgress(progress);
        }
        return progress;
    }

    @Override
    public List<StudyProgress> findByUserId(Long userId) {
        return progressRepository.findByUserId(userId);
    }

    @Override
    public List<StudyProgress> findByPlanId(String planId) {
        return progressRepository.findByPlanId(planId);
    }

    @Override
    public Map<String, Object> getUserStudyStats(Long userId) {
        String cacheKey = STATS_CACHE_PREFIX + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }

        List<StudyProgress> allProgress = progressRepository.findByUserId(userId);
        Map<String, Object> stats = new HashMap<>();
        int total = allProgress.size();
        int completed = 0;
        int inProgress = 0;
        int notStarted = 0;
        int totalDuration = 0;

        for (StudyProgress p : allProgress) {
            String status = p.getStatus();
            if ("COMPLETED".equals(status)) completed++;
            else if ("IN_PROGRESS".equals(status)) inProgress++;
            else notStarted++;
            totalDuration += p.getStudyDuration();
        }

        stats.put("totalKnowledgePoints", total);
        stats.put("completedCount", completed);
        stats.put("inProgressCount", inProgress);
        stats.put("notStartedCount", notStarted);
        stats.put("totalStudyDuration", totalDuration);
        stats.put("completionRate", total > 0 ? (completed * 100 / total) : 0);

        stats.put("totalStudyTime", totalDuration);

        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        Date todayBegin = todayStart.getTime();
        Date now = new Date();

        int todayStudyTime = 0;
        List<StudyProgress> todayRecords = progressRepository.findByUserIdAndLastStudyTimeBetween(userId, todayBegin, now);
        for (StudyProgress p : todayRecords) {
            todayStudyTime += p.getStudyDuration();
        }
        stats.put("todayStudyTime", todayStudyTime);

        int streak = calculateStreak(userId, allProgress);
        stats.put("streak", streak);

        Map<String, Integer> dailyMap = buildDailyMap(userId, 28);
        stats.put("dailyMap", dailyMap);

        List<Map<String, Object>> recentRecords = new ArrayList<>();
        List<StudyProgress> recent = progressRepository.findByUserIdOrderByLastStudyTimeDesc(userId);
        int count = 0;
        for (StudyProgress p : recent) {
            if (count >= 10) break;
            Map<String, Object> record = new HashMap<>();
            record.put("id", p.getId());
            record.put("stageName", p.getKnowledgePointName() != null ? p.getKnowledgePointName() : "学习阶段");
            record.put("planTitle", p.getPlanId());
            record.put("duration", p.getStudyDuration());
            record.put("lastStudyTime", p.getLastStudyTime());
            record.put("progress", p.getProgress());
            recentRecords.add(record);
            count++;
        }
        stats.put("recentRecords", recentRecords);

        redisTemplate.opsForValue().set(cacheKey, stats, 5, TimeUnit.MINUTES);
        return stats;
    }

    @Override
    public void recordStudyEvent(Long userId, String planId, String stageId, String knowledgePointId, int duration) {
        StudyProgress progress = null;

        if (stageId != null && !stageId.isEmpty()) {
            progress = progressRepository.findByUserIdAndStageId(userId, stageId).orElse(null);
        }

        if (progress == null && knowledgePointId != null && !knowledgePointId.isEmpty()) {
            progress = progressRepository.findByUserIdAndKnowledgePointId(userId, knowledgePointId).orElse(null);
        }

        if (progress == null) {
            progress = new StudyProgress();
            progress.setUserId(userId);
            progress.setPlanId(planId);
            progress.setStageId(stageId);
            progress.setKnowledgePointId(knowledgePointId);
            // 使用 stageId 或 knowledgePointId 作为标识名，而非硬编码"阶段学习"
            // 这样知识树可以通过 stageId 匹配到对应阶段的进度
            String kpName = (knowledgePointId != null && !knowledgePointId.isEmpty())
                    ? knowledgePointId
                    : (stageId != null && !stageId.isEmpty())
                        ? stageId
                        : planId;
            progress.setKnowledgePointName(kpName);
            progress.setProgress(0);
            progress.setStudyDuration(0);
            progress.setStatus("NOT_STARTED");
            progress = create(progress);
        }

        int progressIncrease = Math.min(100 - progress.getProgress(), Math.max(1, duration / 2));
        progress.setProgress(Math.min(100, progress.getProgress() + progressIncrease));
        progress.setStudyDuration(progress.getStudyDuration() + duration);
        progress.setLastStudyTime(new Date());
        update(progress);

        invalidateStatsCache(userId);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", userId);
        eventData.put("planId", planId);
        eventData.put("stageId", stageId);
        eventData.put("knowledgePointId", knowledgePointId);
        eventData.put("duration", duration);
        eventData.put("progress", progress.getProgress());
        rabbitTemplate.convertAndSend("study.event.exchange", "study.recorded", eventData);

        log.info("记录学习事件: userId={}, stageId={}, duration={}min, progress={}%", userId, stageId, duration, progress.getProgress());
    }

    private int calculateStreak(Long userId, List<StudyProgress> allProgress) {
        if (allProgress == null || allProgress.isEmpty()) return 0;

        Set<String> studyDays = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (StudyProgress p : allProgress) {
            if (p.getLastStudyTime() != null) {
                studyDays.add(sdf.format(p.getLastStudyTime()));
            }
        }

        if (studyDays.isEmpty()) return 0;

        int streak = 0;
        Calendar cal = Calendar.getInstance();
        String today = sdf.format(cal.getTime());

        if (!studyDays.contains(today)) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        while (true) {
            String dateStr = sdf.format(cal.getTime());
            if (studyDays.contains(dateStr)) {
                streak++;
                cal.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }

        return streak;
    }

    private Map<String, Integer> buildDailyMap(Long userId, int days) {
        Map<String, Integer> dailyMap = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();

        List<StudyProgress> allProgress = progressRepository.findByUserId(userId);
        Map<String, Integer> dayMinutes = new HashMap<>();
        for (StudyProgress p : allProgress) {
            if (p.getLastStudyTime() != null) {
                String day = sdf.format(p.getLastStudyTime());
                dayMinutes.merge(day, p.getStudyDuration(), Integer::sum);
            }
        }

        for (int i = days - 1; i >= 0; i--) {
            Calendar dayCal = Calendar.getInstance();
            dayCal.add(Calendar.DAY_OF_MONTH, -i);
            String key = sdf.format(dayCal.getTime());
            dailyMap.put(key, dayMinutes.getOrDefault(key, 0));
        }

        return dailyMap;
    }

    private void cacheProgress(StudyProgress progress) {
        String cacheKey = PROGRESS_CACHE_PREFIX + progress.getUserId() + ":" + progress.getKnowledgePointId();
        redisTemplate.opsForValue().set(cacheKey, progress, 30, TimeUnit.MINUTES);
    }

    private void invalidateStatsCache(Long userId) {
        redisTemplate.delete(STATS_CACHE_PREFIX + userId);
    }
}
