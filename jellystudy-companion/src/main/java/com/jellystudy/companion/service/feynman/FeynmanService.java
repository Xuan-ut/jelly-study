package com.jellystudy.companion.service.feynman;

/**
 * 费曼反转教学主服务接口
 */
public interface FeynmanService {

    /** 开始费曼教学会话 */
    FeynmanSessionResult startSession(Long userId, String knowledgeId);

    /** 用户解释后AI回应 */
    FeynmanRespondResult respond(String sessionId, String userExplanation);

    /** 获取理解度评估结果 */
    AssessmentResult getAssessment(String sessionId);

    /** 主动结束会话并生成教学总结 */
    AssessmentResult endSession(String sessionId);

    class FeynmanSessionResult {
        private String sessionId;
        private String knowledgeId;
        private String knowledgeName;
        private String aiQuestion;
        private String status;
        private int roundCount;

        public FeynmanSessionResult(String sessionId, String knowledgeId, String knowledgeName,
                                     String aiQuestion, String status, int roundCount) {
            this.sessionId = sessionId;
            this.knowledgeId = knowledgeId;
            this.knowledgeName = knowledgeName;
            this.aiQuestion = aiQuestion;
            this.status = status;
            this.roundCount = roundCount;
        }
        public String getSessionId() { return sessionId; }
        public String getKnowledgeId() { return knowledgeId; }
        public String getKnowledgeName() { return knowledgeName; }
        public String getAiQuestion() { return aiQuestion; }
        public String getStatus() { return status; }
        public int getRoundCount() { return roundCount; }
    }

    class FeynmanRespondResult {
        private String sessionId;
        private int roundNumber;
        private String aiQuestion;
        private double accuracy;
        private double completeness;
        private double depth;
        private double clarity;
        private double abilityToExample;
        private double overallScore;
        private String spiritReaction;
        private boolean sessionComplete;

        // Builder-style setters
        public FeynmanRespondResult sessionId(String v) { this.sessionId = v; return this; }
        public FeynmanRespondResult roundNumber(int v) { this.roundNumber = v; return this; }
        public FeynmanRespondResult aiQuestion(String v) { this.aiQuestion = v; return this; }
        public FeynmanRespondResult accuracy(double v) { this.accuracy = v; return this; }
        public FeynmanRespondResult completeness(double v) { this.completeness = v; return this; }
        public FeynmanRespondResult depth(double v) { this.depth = v; return this; }
        public FeynmanRespondResult clarity(double v) { this.clarity = v; return this; }
        public FeynmanRespondResult abilityToExample(double v) { this.abilityToExample = v; return this; }
        public FeynmanRespondResult overallScore(double v) { this.overallScore = v; return this; }
        public FeynmanRespondResult spiritReaction(String v) { this.spiritReaction = v; return this; }
        public FeynmanRespondResult sessionComplete(boolean v) { this.sessionComplete = v; return this; }

        public String getSessionId() { return sessionId; }
        public int getRoundNumber() { return roundNumber; }
        public String getAiQuestion() { return aiQuestion; }
        public double getAccuracy() { return accuracy; }
        public double getCompleteness() { return completeness; }
        public double getDepth() { return depth; }
        public double getClarity() { return clarity; }
        public double getAbilityToExample() { return abilityToExample; }
        public double getOverallScore() { return overallScore; }
        public String getSpiritReaction() { return spiritReaction; }
        public boolean isSessionComplete() { return sessionComplete; }
    }

    class AssessmentResult {
        private String sessionId;
        private String knowledgeId;
        private double overallScore;
        private java.util.List<String> missingPoints;
        private java.util.List<String> misconceptions;
        private java.util.List<String> recommendedReview;
        private String suggestedNextStep;

        public AssessmentResult() {}
        public AssessmentResult sessionId(String v) { this.sessionId = v; return this; }
        public AssessmentResult knowledgeId(String v) { this.knowledgeId = v; return this; }
        public AssessmentResult overallScore(double v) { this.overallScore = v; return this; }
        public AssessmentResult missingPoints(java.util.List<String> v) { this.missingPoints = v; return this; }
        public AssessmentResult misconceptions(java.util.List<String> v) { this.misconceptions = v; return this; }
        public AssessmentResult recommendedReview(java.util.List<String> v) { this.recommendedReview = v; return this; }
        public AssessmentResult suggestedNextStep(String v) { this.suggestedNextStep = v; return this; }

        public String getSessionId() { return sessionId; }
        public String getKnowledgeId() { return knowledgeId; }
        public double getOverallScore() { return overallScore; }
        public java.util.List<String> getMissingPoints() { return missingPoints; }
        public java.util.List<String> getMisconceptions() { return misconceptions; }
        public java.util.List<String> getRecommendedReview() { return recommendedReview; }
        public String getSuggestedNextStep() { return suggestedNextStep; }
    }
}
