package com.jellystudy.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Answer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String content;
    private String author;
    private Date createTime;
    private Date updateTime;
    private int likeCount;
    private List<Comment> comments;
    private Integer score;
    private String feedback;

    public Answer() {}

    public Answer(String content, String author) {
        this.content = content;
        this.author = author;
        this.createTime = new Date();
        this.updateTime = new Date();
        this.likeCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}