package com.jellystudy.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识树预测 DTO
 */
public class KnowledgeTreePredictionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private LocalDate currentDate;
    private LocalDate targetDate;
    private int daysAhead;
    private BranchState optimistic;
    private BranchState pessimistic;

    public KnowledgeTreePredictionDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getCurrentDate() { return currentDate; }
    public void setCurrentDate(LocalDate currentDate) { this.currentDate = currentDate; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public int getDaysAhead() { return daysAhead; }
    public void setDaysAhead(int daysAhead) { this.daysAhead = daysAhead; }
    public BranchState getOptimistic() { return optimistic; }
    public void setOptimistic(BranchState optimistic) { this.optimistic = optimistic; }
    public BranchState getPessimistic() { return pessimistic; }
    public void setPessimistic(BranchState pessimistic) { this.pessimistic = pessimistic; }

    public static class BranchState implements Serializable {
        private static final long serialVersionUID = 1L;
        private int totalBranches;
        private double avgMastery;
        private String description;
        private List<PredictedBranch> branches = new ArrayList<>();

        public int getTotalBranches() { return totalBranches; }
        public void setTotalBranches(int totalBranches) { this.totalBranches = totalBranches; }
        public double getAvgMastery() { return avgMastery; }
        public void setAvgMastery(double avgMastery) { this.avgMastery = avgMastery; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<PredictedBranch> getBranches() { return branches; }
        public void setBranches(List<PredictedBranch> branches) { this.branches = branches; }
    }

    public static class PredictedBranch implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private double currentProgress;
        private double predictedProgress;
        private double predictedRetention;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getCurrentProgress() { return currentProgress; }
        public void setCurrentProgress(double currentProgress) { this.currentProgress = currentProgress; }
        public double getPredictedProgress() { return predictedProgress; }
        public void setPredictedProgress(double predictedProgress) { this.predictedProgress = predictedProgress; }
        public double getPredictedRetention() { return predictedRetention; }
        public void setPredictedRetention(double predictedRetention) { this.predictedRetention = predictedRetention; }
    }
}
