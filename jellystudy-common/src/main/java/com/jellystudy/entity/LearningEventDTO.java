package com.jellystudy.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 学习事件消息体 DTO（RabbitMQ 传输用）
 */
public class LearningEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private Long userId;
    private String planId;
    private String knowledgeId;
    private Map<String, Object> data = new HashMap<>();
    private LocalDateTime timestamp;

    public LearningEventDTO() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
