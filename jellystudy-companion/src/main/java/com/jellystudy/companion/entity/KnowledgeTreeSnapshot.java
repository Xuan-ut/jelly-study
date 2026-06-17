package com.jellystudy.companion.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "knowledge_tree_snapshot")
public class KnowledgeTreeSnapshot {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private LocalDate date;

    @Builder.Default
    private List<Branch> branches = new ArrayList<>();

    private Prediction prediction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Branch {
        private String name;
        /** 计划ID */
        private String planId;
        /** 计划状态: IN_PROGRESS, COMPLETED */
        private String planStatus;
        /** 计划描述 */
        private String description;
        private double progress;
        private double stability;
        private double predictedRetention30d;
        @Builder.Default
        private List<Node> nodes = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private String name;
        private double mastery;
        private LocalDate lastReview;
        /** 节点类型: PLAN, STAGE, KNOWLEDGE_POINT */
        private String type;
        /** 节点ID（计划ID/阶段ID/知识点名称） */
        private String nodeId;
        /** 节点状态描述 */
        private String status;
        /** 子节点（递归） */
        @Builder.Default
        private List<Node> children = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prediction {
        private Scenario optimistic;
        private Scenario pessimistic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scenario {
        private LocalDate date;
        private int totalBranches;
        private double avgMastery;
        private String description;
    }
}
