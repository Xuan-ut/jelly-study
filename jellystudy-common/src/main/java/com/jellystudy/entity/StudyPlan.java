package com.jellystudy.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class StudyPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Long userId;
    private String title;
    private String description;
    private List<String> knowledgePointIds;
    private List<String> knowledgePointNames;
    private List<PlanStage> stages;
    private String status;
    private int totalProgress;
    private Date createTime;
    private Date updateTime;

    public StudyPlan() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getKnowledgePointIds() { return knowledgePointIds; }
    public void setKnowledgePointIds(List<String> knowledgePointIds) { this.knowledgePointIds = knowledgePointIds; }
    public List<String> getKnowledgePointNames() { return knowledgePointNames; }
    public void setKnowledgePointNames(List<String> knowledgePointNames) { this.knowledgePointNames = knowledgePointNames; }
    public List<PlanStage> getStages() { return stages; }
    public void setStages(List<PlanStage> stages) { this.stages = stages; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalProgress() { return totalProgress; }
    public void setTotalProgress(int totalProgress) { this.totalProgress = totalProgress; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public static class PlanStage implements Serializable {
        private static final long serialVersionUID = 1L;
        private String stageId;
        private String name;
        private String description;
        private List<String> tasks;
        private List<String> knowledgePointIds;
        private int progress;
        private String status;
        private int order;
        private Double estimatedHours;

        public PlanStage() {}

        public String getStageId() { return stageId; }
        public void setStageId(String stageId) { this.stageId = stageId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getTasks() { return tasks; }
        public void setTasks(List<String> tasks) { this.tasks = tasks; }
        public List<String> getKnowledgePointIds() { return knowledgePointIds; }
        public void setKnowledgePointIds(List<String> knowledgePointIds) { this.knowledgePointIds = knowledgePointIds; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
        public Double getEstimatedHours() { return estimatedHours; }
        public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }
    }
}
