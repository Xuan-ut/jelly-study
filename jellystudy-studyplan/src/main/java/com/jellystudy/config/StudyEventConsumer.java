package com.jellystudy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class StudyEventConsumer {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = "study.event.queue")
    public void handleStudyEvent(Message message) {
        try {
            String body = new String(message.getBody());
            Map<String, Object> eventData = objectMapper.readValue(body, Map.class);
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            log.info("收到学习事件: routingKey={}, data={}", routingKey, eventData);

            if (routingKey != null && routingKey.contains("completed")) {
                String userId = String.valueOf(eventData.get("userId"));
                String key = "study:achievements:" + userId;
                redisTemplate.opsForZSet().incrementScore(key, "completed_count", 1);
                log.info("更新用户成就: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("处理学习事件失败: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "study.achievement.queue")
    public void handleAchievement(Message message) {
        try {
            String body = new String(message.getBody());
            Map<String, Object> eventData = objectMapper.readValue(body, Map.class);
            log.info("处理成就事件: {}", eventData);

            String userId = String.valueOf(eventData.get("userId"));
            String key = "study:achievements:" + userId;
            String type = eventData.containsKey("planId") ? "plan_completed" : "knowledge_completed";
            redisTemplate.opsForZSet().incrementScore(key, type, 1);
        } catch (Exception e) {
            log.error("处理成就事件失败: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "study.notification.queue")
    public void handleNotification(Message message) {
        try {
            String body = new String(message.getBody());
            Map<String, Object> eventData = objectMapper.readValue(body, Map.class);
            log.info("发送学习通知: {}", eventData);
        } catch (Exception e) {
            log.error("处理通知事件失败: {}", e.getMessage());
        }
    }
}
