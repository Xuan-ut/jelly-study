package com.jellystudy.entity;

import java.io.Serializable;

/**
 * 关键节点预警 DTO
 */
public class EarlyWarningDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String warningId;
    private String type;
    private String severity;
    private String knowledgePoint;
    private String description;
    private double affectedUserRate;
    private String recommendedAction;

    public EarlyWarningDTO() {}

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
