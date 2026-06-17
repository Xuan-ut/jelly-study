package com.jellystudy.companion.mq.consumer;

import com.jellystudy.companion.config.SpiritConfigProperties;
import com.jellystudy.companion.constant.RedisKeyConstants;
import com.jellystudy.companion.entity.LearningEvent;
import com.jellystudy.companion.service.hive.HiveService;
import com.jellystudy.companion.service.spirit.SpiritGrowService;
import com.jellystudy.companion.util.FeedValueCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 学习事件消费者
 * 监听 study.event.exchange 发布的事件 → 喂养精灵 + 收集群体数据
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StudyEventListener {

    private final SpiritGrowService spiritGrowService;
    private final HiveService hiveService;
    private final SpiritConfigProperties spiritConfig;
    private final StringRedisTemplate redisTemplate;

    /**
     * 监听学习事件 → 喂养精灵
     * Jackson2JsonMessageConverter 自动将 JSON 反序列化为 Map
     */
    @RabbitListener(queues = "companion.spirit.feed")
    public void onLearningEvent(@Payload Map<String, Object> eventData, Message message) {
        try {
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            log.info("[精灵喂养] 收到事件: routingKey={}, data={}", routingKey, eventData);

            Long userId = extractUserId(eventData);
            if (userId == null) {
                log.warn("[精灵喂养] 事件不含userId, 跳过: routingKey={}", routingKey);
                return;
            }

            int feedValue = FeedValueCalculator.calculate(routingKey, spiritConfig);
            if (feedValue > 0) {
                String eventDetail = buildEventDetail(routingKey, eventData);
                spiritGrowService.feedSpirit(userId, routingKey, eventDetail, feedValue);
                // 清除Redis缓存，下次查询会从MongoDB取最新数据
                String cacheKey = RedisKeyConstants.spiritState(userId);
                redisTemplate.delete(cacheKey);
                // 检查是否进化
                spiritGrowService.checkAndTriggerEvolution(userId);
                log.info("[精灵喂养] userId={}, event={}, feedValue={}", userId, routingKey, feedValue);
                updateLeaderboard(userId, feedValue);
            }

            // 收集到Hive
            LearningEvent learningEvent = LearningEvent.builder()
                    .type(routingKey)
                    .userId(userId)
                    .planId(String.valueOf(eventData.getOrDefault("planId", "")))
                    .knowledgeId(String.valueOf(eventData.getOrDefault("knowledgePointId", "")))
                    .data(eventData)
                    .timestamp(LocalDateTime.now())
                    .build();
            hiveService.collectLearningData(learningEvent);

        } catch (Exception e) {
            log.error("[精灵喂养] 处理失败: {}", e.getMessage(), e);
        }
    }

    private String buildEventDetail(String routingKey, Map<String, Object> eventData) {
        if (routingKey == null) return null;
        String planTitle = String.valueOf(eventData.getOrDefault("planTitle", ""));
        String stageName = String.valueOf(eventData.getOrDefault("stageName", ""));
        String taskName = String.valueOf(eventData.getOrDefault("taskName", ""));
        String kpName = String.valueOf(eventData.getOrDefault("knowledgePointName", ""));
        if (routingKey.contains("task") && !taskName.isEmpty() && !"null".equals(taskName))
            return "完成任务「" + taskName + "」";
        if (routingKey.contains("stage") && !stageName.isEmpty() && !"null".equals(stageName))
            return "完成阶段「" + stageName + "」";
        if (routingKey.contains("plan") && !planTitle.isEmpty() && !"null".equals(planTitle))
            return "完成计划「" + planTitle + "」";
        if (!kpName.isEmpty() && !"null".equals(kpName))
            return "学习知识点「" + kpName + "」";
        return null;
    }

    private Long extractUserId(Map<String, Object> eventData) {
        if (eventData.containsKey("userId") && eventData.get("userId") != null) {
            Object uid = eventData.get("userId");
            if (uid instanceof Number) return ((Number) uid).longValue();
            try { return Long.valueOf(String.valueOf(uid)); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private void updateLeaderboard(Long userId, int score) {
        try {
            String key = RedisKeyConstants.LEADERBOARD_WEEKLY;
            redisTemplate.opsForZSet().incrementScore(key, userId.toString(), score);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("更新排行榜失败: userId={}", userId, e);
        }
    }
}
