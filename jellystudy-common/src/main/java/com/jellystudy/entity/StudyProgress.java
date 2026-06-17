package com.jellystudy.entity;

import java.io.Serializable;
import java.util.Date;

public class StudyProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Long userId;
    private String planId;
    private String stageId;
    private String knowledgePointId;
    private String knowledgePointName;
    private int progress;
    private String status;
    private int studyDuration;
    private Date lastStudyTime;
    private Date createTime;
    private Date updateTime;

    public StudyProgress() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getStageId() { return stageId; }
    public void setStageId(String stageId) { this.stageId = stageId; }
    public String getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(String knowledgePointId) { this.knowledgePointId = knowledgePointId; }
    public String getKnowledgePointName() { return knowledgePointName; }
    public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getStudyDuration() { return studyDuration; }
    public void setStudyDuration(int studyDuration) { this.studyDuration = studyDuration; }
    public Date getLastStudyTime() { return lastStudyTime; }
    public void setLastStudyTime(Date lastStudyTime) { this.lastStudyTime = lastStudyTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
