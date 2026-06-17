package com.jellystudy.entity;

import java.io.Serializable;
import java.util.Date;

public class AnswerEvaluation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String questionId;
    private String answerId;
    private int score;
    private String scoreType;
    private String feedback;
    private Date evaluateTime;

    public AnswerEvaluation() {}

    public AnswerEvaluation(String questionId, String answerId, int score, String scoreType, String feedback) {
        this.questionId = questionId;
        this.answerId = answerId;
        this.score = score;
        this.scoreType = scoreType;
        this.feedback = feedback;
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

    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Date getEvaluateTime() {
        return evaluateTime;
    }

    public void setEvaluateTime(Date evaluateTime) {
        this.evaluateTime = evaluateTime;
    }
}