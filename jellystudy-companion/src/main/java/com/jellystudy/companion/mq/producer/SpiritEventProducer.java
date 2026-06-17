package com.jellystudy.companion.mq.producer;

import com.jellystudy.companion.constant.MQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 精灵事件生产者
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SpiritEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布精灵升级事件
     */
    public void publishSpiritLevelUp(Long userId, int newLevel) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("newLevel", newLevel);
        event.put("timestamp", LocalDateTime.now().toString());

        rabbitTemplate.convertAndSend(
                MQConstants.EXCHANGE_LEARNING_EVENT,
                MQConstants.RK_SPIRIT_LEVELUP,
                event
        );
        log.info("发布精灵升级事件: userId={}, newLevel={}", userId, newLevel);
    }

    /**
     * 发布精灵情感变化事件
     */
    public void publishSpiritEmotionChange(Long userId, String oldEmotion, String newEmotion) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("oldEmotion", oldEmotion);
        event.put("newEmotion", newEmotion);
        event.put("timestamp", LocalDateTime.now().toString());

        rabbitTemplate.convertAndSend(
                MQConstants.EXCHANGE_LEARNING_EVENT,
                MQConstants.RK_SPIRIT_EMOTION_CHANGE,
                event
        );
        log.info("发布精灵情感变化事件: userId={}, {}→{}", userId, oldEmotion, newEmotion);
    }
}
