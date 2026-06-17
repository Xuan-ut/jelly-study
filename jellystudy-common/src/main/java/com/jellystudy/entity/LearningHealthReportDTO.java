package com.jellystudy.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习健康报告 DTO
 */
public class LearningHealthReportDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String reportDate;
    private double overallScore;
    private double averageDailyMinutes;
    private double taskCompletionRate;
    private int streakDays;
    private int totalDays;
    private List<String> suggestions = new ArrayList<>();
    private List<String> anomalyFlags = new ArrayList<>();

    public LearningHealthReportDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public double getAverageDailyMinutes() { return averageDailyMinutes; }
    public void setAverageDailyMinutes(double averageDailyMinutes) { this.averageDailyMinutes = averageDailyMinutes; }
    public double getTaskCompletionRate() { return taskCompletionRate; }
    public void setTaskCompletionRate(double taskCompletionRate) { this.taskCompletionRate = taskCompletionRate; }
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public List<String> getAnomalyFlags() { return anomalyFlags; }
    public void setAnomalyFlags(List<String> anomalyFlags) { this.anomalyFlags = anomalyFlags; }
}
