package com.jellystudy.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习轨迹回溯 DTO
 */
public class LearningTimelineDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private List<TimelineEntry> entries = new ArrayList<>();
    private List<Milestone> milestones = new ArrayList<>();

    public LearningTimelineDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<TimelineEntry> getEntries() { return entries; }
    public void setEntries(List<TimelineEntry> entries) { this.entries = entries; }
    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }

    public static class TimelineEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private String event;
        private String description;
        private List<String> knowledgePoints = new ArrayList<>();
        private double cumulativeHours;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getKnowledgePoints() { return knowledgePoints; }
        public void setKnowledgePoints(List<String> knowledgePoints) { this.knowledgePoints = knowledgePoints; }
        public double getCumulativeHours() { return cumulativeHours; }
        public void setCumulativeHours(double cumulativeHours) { this.cumulativeHours = cumulativeHours; }
    }

    public static class Milestone implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private String title;
        private String description;
        private String icon;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
}
