package com.jellystudy.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 精灵对话响应 DTO
 */
public class SpiritChatResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String spiritMessage;
    private String emotion;
    private List<String> suggestedTopics = new ArrayList<>();

    public SpiritChatResponseDTO() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSpiritMessage() { return spiritMessage; }
    public void setSpiritMessage(String spiritMessage) { this.spiritMessage = spiritMessage; }
    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    public List<String> getSuggestedTopics() { return suggestedTopics; }
    public void setSuggestedTopics(List<String> suggestedTopics) { this.suggestedTopics = suggestedTopics; }
}
