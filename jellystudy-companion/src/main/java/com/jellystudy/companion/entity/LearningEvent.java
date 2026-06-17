package com.jellystudy.companion.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息体 - 学习事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private Long userId;
    private String planId;
    private String knowledgeId;
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
    private LocalDateTime timestamp;
}
