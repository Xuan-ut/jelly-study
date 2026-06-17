package com.jellystudy.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识树快照 DTO
 */
public class KnowledgeTreeSnapshotDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Long userId;
    private LocalDate date;
    private List<Branch> branches = new ArrayList<>();
    private Prediction prediction;

    public KnowledgeTreeSnapshotDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<Branch> getBranches() { return branches; }
    public void setBranches(List<Branch> branches) { this.branches = branches; }
    public Prediction getPrediction() { return prediction; }
    public void setPrediction(Prediction prediction) { this.prediction = prediction; }

    public static class Branch implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String planId;
        private String planStatus;
        private String description;
        private double progress;
        private double stability;
        private double predictedRetention30d;
        private List<Node> nodes = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public String getPlanStatus() { return planStatus; }
        public void setPlanStatus(String planStatus) { this.planStatus = planStatus; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
        public double getStability() { return stability; }
        public void setStability(double stability) { this.stability = stability; }
        public double getPredictedRetention30d() { return predictedRetention30d; }
        public void setPredictedRetention30d(double predictedRetention30d) { this.predictedRetention30d = predictedRetention30d; }
        public List<Node> getNodes() { return nodes; }
        public void setNodes(List<Node> nodes) { this.nodes = nodes; }
    }

    public static class Node implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private double mastery;
        private LocalDate lastReview;
        private String type;
        private String nodeId;
        private String status;
        private List<Node> children = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getMastery() { return mastery; }
        public void setMastery(double mastery) { this.mastery = mastery; }
        public LocalDate getLastReview() { return lastReview; }
        public void setLastReview(LocalDate lastReview) { this.lastReview = lastReview; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<Node> getChildren() { return children; }
        public void setChildren(List<Node> children) { this.children = children; }
    }

    public static class Prediction implements Serializable {
        private static final long serialVersionUID = 1L;
        private Scenario optimistic;
        private Scenario pessimistic;

        public Scenario getOptimistic() { return optimistic; }
        public void setOptimistic(Scenario optimistic) { this.optimistic = optimistic; }
        public Scenario getPessimistic() { return pessimistic; }
        public void setPessimistic(Scenario pessimistic) { this.pessimistic = pessimistic; }
    }

    public static class Scenario implements Serializable {
        private static final long serialVersionUID = 1L;
        private LocalDate date;
        private int totalBranches;
        private double avgMastery;
        private String description;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public int getTotalBranches() { return totalBranches; }
        public void setTotalBranches(int totalBranches) { this.totalBranches = totalBranches; }
        public double getAvgMastery() { return avgMastery; }
        public void setAvgMastery(double avgMastery) { this.avgMastery = avgMastery; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
