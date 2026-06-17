package com.jellystudy.companion.service.hive;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.companion.config.CacheConfigProperties;
import com.jellystudy.companion.constant.RedisKeyConstants;
import com.jellystudy.companion.entity.AnomalyRecord;
import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.entity.LearningEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 群体智慧蜂巢主服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HiveServiceImpl implements HiveService {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final PatternDiscoveryService patternDiscoveryService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final StringRedisTemplate redisTemplate;
    private final CacheConfigProperties cacheConfig;

    @Override
    @SneakyThrows
    public void collectLearningData(LearningEvent event) {
        log.info("收集学习数据: userId={}, type={}", event.getUserId(), event.getType());
        String key = "companion:hive:buffer:" + event.getType();
        redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(event));
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    @Override
    @SneakyThrows
    public List<HivePattern> getPatterns(String subject) {
        String key = RedisKeyConstants.hivePattern(subject);
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            log.debug("群体模式命中缓存: subject={}", subject);
            return objectMapper.readValue(cached, new TypeReference<List<HivePattern>>() {});
        }
        List<HivePattern> patterns = patternDiscoveryService.getPatternsBySubject(subject);
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(patterns),
                cacheConfig.getPatternTtl(), TimeUnit.MINUTES);
        return patterns;
    }

    @Override
    public AnomalyRecord getAnomalyReport(Long userId) {
        return anomalyDetectionService.detectAnomalies(userId);
    }
}
