package com.jellystudy.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 费曼教学会话 DTO
 */
public class FeynmanSessionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private Long userId;
    private String knowledgeId;
    private String knowledgeName;
    private String aiQuestion;
    private String status;
    private int roundCount;
    private LocalDateTime startTime;

    public FeynmanSessionDTO() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getKnowledgeName() { return knowledgeName; }
    public void setKnowledgeName(String knowledgeName) { this.knowledgeName = knowledgeName; }
    public String getAiQuestion() { return aiQuestion; }
    public void setAiQuestion(String aiQuestion) { this.aiQuestion = aiQuestion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRoundCount() { return roundCount; }
    public void setRoundCount(int roundCount) { this.roundCount = roundCount; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
}
