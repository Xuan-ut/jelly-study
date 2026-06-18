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
    /** 意图：normal / generated_plan */
    private String intent;
    /** 已生成计划的id（intent=generated_plan时有值） */
    private String planId;
    /** 已生成计划的标题 */
    private String planTitle;

    public SpiritChatResponseDTO() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSpiritMessage() { return spiritMessage; }
    public void setSpiritMessage(String spiritMessage) { this.spiritMessage = spiritMessage; }
    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    public List<String> getSuggestedTopics() { return suggestedTopics; }
    public void setSuggestedTopics(List<String> suggestedTopics) { this.suggestedTopics = suggestedTopics; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getPlanTitle() { return planTitle; }
    public void setPlanTitle(String planTitle) { this.planTitle = planTitle; }
}
