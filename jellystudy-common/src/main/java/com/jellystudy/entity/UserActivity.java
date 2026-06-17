package com.jellystudy.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class UserActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Long userId;
    private String activityType;
    private String targetId;
    private String targetTitle;
    private String content;
    private Map<String, Object> metadata;
    private Date createTime;

    public static final String TYPE_QUESTION = "QUESTION";
    public static final String TYPE_ANSWER = "ANSWER";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_BROWSE = "BROWSE";
    public static final String TYPE_STUDY = "STUDY";
    public static final String TYPE_PLAN_CREATE = "PLAN_CREATE";
    public static final String TYPE_PLAN_COMPLETE = "PLAN_COMPLETE";

    public UserActivity() {}

    public UserActivity(Long userId, String activityType, String targetId, String targetTitle, String content) {
        this.userId = userId;
        this.activityType = activityType;
        this.targetId = targetId;
        this.targetTitle = targetTitle;
        this.content = content;
        this.createTime = new Date();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetTitle() { return targetTitle; }
    public void setTargetTitle(String targetTitle) { this.targetTitle = targetTitle; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
