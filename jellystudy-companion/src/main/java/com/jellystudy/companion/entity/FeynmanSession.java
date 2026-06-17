package com.jellystudy.companion.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feynman_session")
public class FeynmanSession {

    @Id
    private String id;

    private Long userId;

    @Indexed(unique = true)
    private String sessionId;

    private String knowledgeId;

    private String knowledgeName;

    /** IN_PROGRESS / COMPLETED / ABANDONED */
    private String status;

    @Builder.Default
    private List<Round> rounds = new ArrayList<>();

    private FinalAssessment finalAssessment;

    private String spiritReaction;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Round {
        private int roundNumber;
        private String userExplanation;
        private String aiQuestion;
        private RoundAssessment assessment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundAssessment {
        private double accuracy;
        private double completeness;
        private double depth;
        private double clarity;
        private double abilityToExample;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalAssessment {
        private double overallScore;
        @Builder.Default
        private List<String> missingPoints = new ArrayList<>();
        @Builder.Default
        private List<String> misconceptions = new ArrayList<>();
        @Builder.Default
        private List<String> recommendedReview = new ArrayList<>();
        private String suggestedNextStep;
    }
}
