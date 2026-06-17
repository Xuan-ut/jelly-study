package com.jellystudy.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 精灵状态 DTO
 */
public class SpiritStateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Long userId;
    private String name;
    private int level;
    private String levelName;
    private int experience;
    private int nextLevelExp;
    private int satiation;
    private String emotion;
    private String personalityKey;
    private int appearanceLevel;
    private Appearance appearance;
    private List<String> skills = new ArrayList<>();
    private Memory memory;
    private List<GrowthLog> growthLog = new ArrayList<>();
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public SpiritStateDTO() {}

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public int getNextLevelExp() { return nextLevelExp; }
    public void setNextLevelExp(int nextLevelExp) { this.nextLevelExp = nextLevelExp; }
    public int getSatiation() { return satiation; }
    public void setSatiation(int satiation) { this.satiation = satiation; }
    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    public String getPersonalityKey() { return personalityKey; }
    public void setPersonalityKey(String personalityKey) { this.personalityKey = personalityKey; }
    public int getAppearanceLevel() { return appearanceLevel; }
    public void setAppearanceLevel(int appearanceLevel) { this.appearanceLevel = appearanceLevel; }
    public Appearance getAppearance() { return appearance; }
    public void setAppearance(Appearance appearance) { this.appearance = appearance; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public Memory getMemory() { return memory; }
    public void setMemory(Memory memory) { this.memory = memory; }
    public List<GrowthLog> getGrowthLog() { return growthLog; }
    public void setGrowthLog(List<GrowthLog> growthLog) { this.growthLog = growthLog; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    // --- Inner classes ---
    public static class Appearance implements Serializable {
        private static final long serialVersionUID = 1L;
        private String body;
        private String wings;
        private String aura;
        private String crown;

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getWings() { return wings; }
        public void setWings(String wings) { this.wings = wings; }
        public String getAura() { return aura; }
        public void setAura(String aura) { this.aura = aura; }
        public String getCrown() { return crown; }
        public void setCrown(String crown) { this.crown = crown; }
    }

    public static class Memory implements Serializable {
        private static final long serialVersionUID = 1L;
        private LocalDateTime lastInteraction;
        private List<String> recentTopics = new ArrayList<>();
        private List<String> userPreferences = new ArrayList<>();

        public LocalDateTime getLastInteraction() { return lastInteraction; }
        public void setLastInteraction(LocalDateTime lastInteraction) { this.lastInteraction = lastInteraction; }
        public List<String> getRecentTopics() { return recentTopics; }
        public void setRecentTopics(List<String> recentTopics) { this.recentTopics = recentTopics; }
        public List<String> getUserPreferences() { return userPreferences; }
        public void setUserPreferences(List<String> userPreferences) { this.userPreferences = userPreferences; }
    }

    public static class GrowthLog implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private String event;
        private String detail;
        private int expGained;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public int getExpGained() { return expGained; }
        public void setExpGained(int expGained) { this.expGained = expGained; }
    }
}
