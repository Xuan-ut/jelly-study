package com.jellystudy.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Document(collection = "daily_tasks")
public class DailyTaskBoard implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private Long userId;
    private String date;
    private List<DailyTask> tasks;
    private Date createTime;
    private Date updateTime;

    public DailyTaskBoard() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public List<DailyTask> getTasks() { return tasks; }
    public void setTasks(List<DailyTask> tasks) { this.tasks = tasks; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public static class DailyTask implements Serializable {
        private static final long serialVersionUID = 1L;
        private String taskId;
        private String content;
        private boolean completed;
        private String planId;
        private String stageId;
        private int order;

        public DailyTask() {}

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public String getStageId() { return stageId; }
        public void setStageId(String stageId) { this.stageId = stageId; }
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }
}
