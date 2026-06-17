package com.jellystudy.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 个体异常检测报告 DTO
 */
public class AnomalyReportDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String reportDate;
    private List<AnomalyDimension> dimensions = new ArrayList<>();
    private String overallRisk;
    private String overallRiskLevel;

    public AnomalyReportDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }
    public List<AnomalyDimension> getDimensions() { return dimensions; }
    public void setDimensions(List<AnomalyDimension> dimensions) { this.dimensions = dimensions; }
    public String getOverallRisk() { return overallRisk; }
    public void setOverallRisk(String overallRisk) { this.overallRisk = overallRisk; }
    public String getOverallRiskLevel() { return overallRiskLevel; }
    public void setOverallRiskLevel(String overallRiskLevel) { this.overallRiskLevel = overallRiskLevel; }

    public static class AnomalyDimension implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private double userValue;
        private String normalRange;
        private String status;
        private String recommendation;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getUserValue() { return userValue; }
        public void setUserValue(double userValue) { this.userValue = userValue; }
        public String getNormalRange() { return normalRange; }
        public void setNormalRange(String normalRange) { this.normalRange = normalRange; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }
}
