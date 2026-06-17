package com.jellystudy.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群体学习模式 DTO
 */
public class HivePatternDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String patternId;
    private String type;
    private String subject;
    private String knowledgeId;
    private String description;
    private Map<String, Object> statistics = new HashMap<>();
    private List<Intervention> interventions = new ArrayList<>();
    private String discoveredAt;
    private double confidence;

    public HivePatternDTO() {}

    public String getPatternId() { return patternId; }
    public void setPatternId(String patternId) { this.patternId = patternId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, Object> getStatistics() { return statistics; }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }
    public List<Intervention> getInterventions() { return interventions; }
    public void setInterventions(List<Intervention> interventions) { this.interventions = interventions; }
    public String getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(String discoveredAt) { this.discoveredAt = discoveredAt; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public static class Intervention implements Serializable {
        private static final long serialVersionUID = 1L;
        private String trigger;
        private String action;
        private double effectiveness;

        public String getTrigger() { return trigger; }
        public void setTrigger(String trigger) { this.trigger = trigger; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public double getEffectiveness() { return effectiveness; }
        public void setEffectiveness(double effectiveness) { this.effectiveness = effectiveness; }
    }
}
