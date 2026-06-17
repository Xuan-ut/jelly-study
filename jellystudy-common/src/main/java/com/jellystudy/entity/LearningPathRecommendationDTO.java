package com.jellystudy.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习路径推荐 DTO
 */
public class LearningPathRecommendationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pathId;
    private String name;
    private List<String> steps = new ArrayList<>();
    private double successRate;
    private int avgDays;
    private String suitableFor;
    private boolean recommended;
    private String warning;

    public LearningPathRecommendationDTO() {}

    public String getPathId() { return pathId; }
    public void setPathId(String pathId) { this.pathId = pathId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    public int getAvgDays() { return avgDays; }
    public void setAvgDays(int avgDays) { this.avgDays = avgDays; }
    public String getSuitableFor() { return suitableFor; }
    public void setSuitableFor(String suitableFor) { this.suitableFor = suitableFor; }
    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }
    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }
}
