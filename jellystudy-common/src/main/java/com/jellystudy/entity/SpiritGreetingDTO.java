package com.jellystudy.entity;

import java.io.Serializable;

/**
 * 精灵问候语 DTO
 */
public class SpiritGreetingDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String emotion;
    private String greeting;
    private String suggestion;

    public SpiritGreetingDTO() {}

    public SpiritGreetingDTO(String emotion, String greeting, String suggestion) {
        this.emotion = emotion;
        this.greeting = greeting;
        this.suggestion = suggestion;
    }

    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}
