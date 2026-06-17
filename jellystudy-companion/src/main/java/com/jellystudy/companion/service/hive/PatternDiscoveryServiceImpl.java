package com.jellystudy.companion.service.hive;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.companion.ai.AIClient;
import com.jellystudy.companion.ai.prompt.HivePromptBuilder;
import com.jellystudy.companion.config.HiveConfigProperties;
import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.enums.HivePatternType;
import com.jellystudy.companion.repository.HivePatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 群体模式发现服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PatternDiscoveryServiceImpl implements PatternDiscoveryService {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final HivePatternRepository patternRepository;
    private final AIClient aiClient;
    private final HivePromptBuilder promptBuilder;
    private final StringRedisTemplate redisTemplate;
    private final HiveConfigProperties hiveConfig;

    @Override
    @SneakyThrows
    public void discoverPatterns() {
        log.info("开始群体模式发现...");

        Set<String> keys = redisTemplate.keys("companion:hive:buffer:*");
        if (keys == null || keys.isEmpty()) {
            log.info("无学习数据，跳过模式发现");
            return;
        }

        Map<String, List<Map<String, Object>>> subjectData = new HashMap<>();
        for (String key : keys) {
            List<String> events = redisTemplate.opsForList().range(key, 0, -1);
            if (events != null) {
                for (String eventJson : events) {
                    try {
                        Map<String, Object> event = objectMapper.readValue(eventJson,
                                new TypeReference<Map<String, Object>>() {});
                        String subject = "Java";
                        subjectData.computeIfAbsent(subject, k -> new ArrayList<>()).add(event);
                    } catch (Exception e) {
                        log.warn("解析学习事件失败: {}", e.getMessage());
                    }
                }
            }
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : subjectData.entrySet()) {
            if (entry.getValue().size() < hiveConfig.getMinSampleSize()) {
                log.info("学科'{}'样本量不足({}<{}), 跳过", entry.getKey(),
                        entry.getValue().size(), hiveConfig.getMinSampleSize());
                continue;
            }

            String dataSummary = buildDataSummary(entry.getValue());
            String prompt = promptBuilder.buildPatternDiscoveryPrompt(entry.getKey(), dataSummary);
            String aiResponse = aiClient.chat("群体模式发现", prompt);
            parseAndSavePatterns(entry.getKey(), aiResponse);
        }

        log.info("群体模式发现完成，处理了{}个学科", subjectData.size());
    }

    @Override
    public List<HivePattern> getPatternsByType(HivePatternType type) {
        return patternRepository.findByType(type);
    }

    @Override
    public List<HivePattern> getPatternsBySubject(String subject) {
        return patternRepository.findBySubject(subject);
    }

    private String buildDataSummary(List<Map<String, Object>> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("事件总数: ").append(events.size()).append("\n");
        Map<String, Long> typeCount = events.stream()
                .collect(Collectors.groupingBy(
                        e -> String.valueOf(e.getOrDefault("type", "unknown")),
                        Collectors.counting()));
        sb.append("事件类型分布: ").append(typeCount).append("\n");
        Map<String, Long> userCount = events.stream()
                .collect(Collectors.groupingBy(
                        e -> String.valueOf(e.getOrDefault("userId", "unknown")),
                        Collectors.counting()));
        sb.append("活跃用户数: ").append(userCount.size());
        return sb.toString();
    }

    private void parseAndSavePatterns(String subject, String aiResponse) {
        if (aiResponse != null && !aiResponse.contains("未发现新模式")) {
            HivePattern pattern = HivePattern.builder()
                    .patternId(UUID.randomUUID().toString().substring(0, 8))
                    .type(HivePatternType.BOTTLENECK)
                    .subject(subject)
                    .description(aiResponse.substring(0, Math.min(200, aiResponse.length())))
                    .discoveredAt(LocalDate.now().toString())
                    .confidence(0.8)
                    .build();
            patternRepository.save(pattern);
            log.info("保存新模式: patternId={}", pattern.getPatternId());
        }
    }
}
