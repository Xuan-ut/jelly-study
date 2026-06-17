package com.jellystudy.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 理解度评估 DTO
 */
public class UnderstandingAssessmentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String knowledgeId;
    private double overallScore;
    private double accuracy;
    private double completeness;
    private double depth;
    private double clarity;
    private double abilityToExample;
    private List<String> missingPoints = new ArrayList<>();
    private List<String> misconceptions = new ArrayList<>();
    private List<String> recommendedReview = new ArrayList<>();
    private String suggestedNextStep;

    public UnderstandingAssessmentDTO() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public double getCompleteness() { return completeness; }
    public void setCompleteness(double completeness) { this.completeness = completeness; }
    public double getDepth() { return depth; }
    public void setDepth(double depth) { this.depth = depth; }
    public double getClarity() { return clarity; }
    public void setClarity(double clarity) { this.clarity = clarity; }
    public double getAbilityToExample() { return abilityToExample; }
    public void setAbilityToExample(double abilityToExample) { this.abilityToExample = abilityToExample; }
    public List<String> getMissingPoints() { return missingPoints; }
    public void setMissingPoints(List<String> missingPoints) { this.missingPoints = missingPoints; }
    public List<String> getMisconceptions() { return misconceptions; }
    public void setMisconceptions(List<String> misconceptions) { this.misconceptions = misconceptions; }
    public List<String> getRecommendedReview() { return recommendedReview; }
    public void setRecommendedReview(List<String> recommendedReview) { this.recommendedReview = recommendedReview; }
    public String getSuggestedNextStep() { return suggestedNextStep; }
    public void setSuggestedNextStep(String suggestedNextStep) { this.suggestedNextStep = suggestedNextStep; }
}
