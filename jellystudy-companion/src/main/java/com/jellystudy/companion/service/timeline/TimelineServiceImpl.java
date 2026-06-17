package com.jellystudy.companion.service.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.companion.config.CacheConfigProperties;
import com.jellystudy.companion.constant.RedisKeyConstants;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 时空预测主服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TimelineServiceImpl implements TimelineService {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final KnowledgeTreeService knowledgeTreeService;
    private final ForgettingCurveService forgettingCurveService;
    private final EarlyWarningService earlyWarningService;
    private final StringRedisTemplate redisTemplate;
    private final CacheConfigProperties cacheConfig;

    @Override
    @SneakyThrows
    public KnowledgeTreeSnapshot getKnowledgeTree(Long userId) {
        // 始终重建知识树以保证计划变更能实时反映
        KnowledgeTreeSnapshot snapshot = knowledgeTreeService.buildTree(userId);
        forgettingCurveService.calculateAllRetentions(userId);
        // 写入缓存，短TTL
        String key = RedisKeyConstants.knowledgeTree(userId);
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(snapshot),
                5, TimeUnit.MINUTES);
        return snapshot;
    }

    @Override
    public KnowledgeTreePrediction predictKnowledgeTree(Long userId, int daysAhead) {
        return knowledgeTreeService.getTreePrediction(userId, daysAhead);
    }

    @Override
    public LearningTimeline getLearningTimeline(Long userId) {
        LearningTimeline timeline = new LearningTimeline();
        timeline.setUserId(userId);
        return timeline;
    }

    @Override
    public List<EarlyWarning> getEarlyWarnings(Long userId) {
        return earlyWarningService.checkWarnings(userId);
    }
}
