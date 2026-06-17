package com.jellystudy.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class QuestionEvaluation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String questionId;
    private List<String> knowledgePoints;
    private String difficulty;
    private Date evaluateTime;

    public QuestionEvaluation() {}

    public QuestionEvaluation(String questionId, List<String> knowledgePoints, String difficulty) {
        this.questionId = questionId;
        this.knowledgePoints = knowledgePoints;
        this.difficulty = difficulty;
        this.evaluateTime = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public List<String> getKnowledgePoints() {
        return knowledgePoints;
    }

    public void setKnowledgePoints(List<String> knowledgePoints) {
        this.knowledgePoints = knowledgePoints;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Date getEvaluateTime() {
        return evaluateTime;
    }

    public void setEvaluateTime(Date evaluateTime) {
        this.evaluateTime = evaluateTime;
    }
}