package com.jellystudy.companion.service.timeline;

import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;

import java.util.List;

/**
 * 时空预测主服务接口
 */
public interface TimelineService {

    KnowledgeTreeSnapshot getKnowledgeTree(Long userId);

    KnowledgeTreePrediction predictKnowledgeTree(Long userId, int daysAhead);

    LearningTimeline getLearningTimeline(Long userId);

    List<EarlyWarning> getEarlyWarnings(Long userId);

    // --- 结果类 ---
    class KnowledgeTreePrediction {
        private KnowledgeTreeSnapshot current;
        private KnowledgeTreeSnapshot optimistic;
        private KnowledgeTreeSnapshot pessimistic;

        public KnowledgeTreePrediction current(KnowledgeTreeSnapshot v) { this.current = v; return this; }
        public KnowledgeTreePrediction optimistic(KnowledgeTreeSnapshot v) { this.optimistic = v; return this; }
        public KnowledgeTreePrediction pessimistic(KnowledgeTreeSnapshot v) { this.pessimistic = v; return this; }
        public KnowledgeTreeSnapshot getCurrent() { return current; }
        public KnowledgeTreeSnapshot getOptimistic() { return optimistic; }
        public KnowledgeTreeSnapshot getPessimistic() { return pessimistic; }
    }

    class LearningTimeline {
        private Long userId;
        private List<TimelineEntry> entries;
        private List<Milestone> milestones;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public List<TimelineEntry> getEntries() { return entries; }
        public void setEntries(List<TimelineEntry> entries) { this.entries = entries; }
        public List<Milestone> getMilestones() { return milestones; }
        public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }
    }

    class TimelineEntry {
        private String date;
        private String event;
        private String description;
        private List<String> knowledgePoints;
        private double cumulativeHours;
        // getters and setters
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

    class Milestone {
        private String date;
        private String title;
        private String description;
        private String icon;
        // getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }

    class EarlyWarning {
        private String warningId;
        private String type;
        private String severity;
        private String knowledgePoint;
        private String description;
        private double affectedUserRate;
        private String recommendedAction;
        // getters and setters
        public String getWarningId() { return warningId; }
        public void setWarningId(String warningId) { this.warningId = warningId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getKnowledgePoint() { return knowledgePoint; }
        public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getAffectedUserRate() { return affectedUserRate; }
        public void setAffectedUserRate(double affectedUserRate) { this.affectedUserRate = affectedUserRate; }
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    }
}
