package com.jellystudy.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String title;
    private String content;
    private String author;
    private Date createTime;
    private Date updateTime;
    private List<String> knowledgePointIds;
    private int likeCount;
    private int answerCount;
    private List<Answer> answers;
    private String difficulty;
    private List<String> knowledgePoints;

    public Question() {}

    public Question(String title, String content, String author, List<String> knowledgePointIds) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createTime = new Date();
        this.updateTime = new Date();
        this.knowledgePointIds = knowledgePointIds;
        this.likeCount = 0;
        this.answerCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<String> getKnowledgePointIds() {
        return knowledgePointIds;
    }

    public void setKnowledgePointIds(List<String> knowledgePointIds) {
        this.knowledgePointIds = knowledgePointIds;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public void setAnswerCount(int answerCount) {
        this.answerCount = answerCount;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getKnowledgePoints() {
        return knowledgePoints;
    }

    public void setKnowledgePoints(List<String> knowledgePoints) {
        this.knowledgePoints = knowledgePoints;
    }
}