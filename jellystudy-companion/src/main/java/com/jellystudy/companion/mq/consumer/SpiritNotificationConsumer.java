package com.jellystudy.companion.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 精灵通知消费者
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SpiritNotificationConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @RabbitListener(queues = "companion.spirit.notify")
    public void onSpiritNotification(Message message) {
        try {
            String body = new String(message.getBody());
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            Map<String, Object> eventData = objectMapper.readValue(body, Map.class);

            log.info("收到精灵通知: routingKey={}, data={}", routingKey, eventData);

            if ("spirit.levelup".equals(routingKey)) {
                Long userId = Long.valueOf(String.valueOf(eventData.get("userId")));
                int newLevel = ((Number) eventData.get("newLevel")).intValue();
                log.info("精灵升级通知: userId={}, newLevel={}", userId, newLevel);
            } else if ("spirit.emotion.change".equals(routingKey)) {
                log.info("精灵情感变化通知: {}", eventData);
            }
        } catch (Exception e) {
            log.error("处理精灵通知失败: {}", e.getMessage(), e);
        }
    }
}
