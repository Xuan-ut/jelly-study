package com.jellystudy.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 费曼教学回应 DTO
 */
public class FeynmanResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private int roundNumber;
    private String aiQuestion;
    private Assessment assessment;
    private String spiritReaction;
    private boolean sessionComplete;

    public FeynmanResponseDTO() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public String getAiQuestion() { return aiQuestion; }
    public void setAiQuestion(String aiQuestion) { this.aiQuestion = aiQuestion; }
    public Assessment getAssessment() { return assessment; }
    public void setAssessment(Assessment assessment) { this.assessment = assessment; }
    public String getSpiritReaction() { return spiritReaction; }
    public void setSpiritReaction(String spiritReaction) { this.spiritReaction = spiritReaction; }
    public boolean isSessionComplete() { return sessionComplete; }
    public void setSessionComplete(boolean sessionComplete) { this.sessionComplete = sessionComplete; }

    public static class Assessment implements Serializable {
        private static final long serialVersionUID = 1L;
        private double accuracy;
        private double completeness;
        private double depth;
        private double clarity;
        private double abilityToExample;
        private double overallScore;
        private List<String> missingPoints = new ArrayList<>();
        private List<String> misconceptions = new ArrayList<>();

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
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
        public List<String> getMissingPoints() { return missingPoints; }
        public void setMissingPoints(List<String> missingPoints) { this.missingPoints = missingPoints; }
        public List<String> getMisconceptions() { return misconceptions; }
        public void setMisconceptions(List<String> misconceptions) { this.misconceptions = misconceptions; }
    }
}
