package com.jellystudy.companion.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 返回的理解度评估结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResult {
    private double overallScore;
    @Builder.Default
    private Map<String, Double> dimensions = new HashMap<>();
    @Builder.Default
    private List<String> missingPoints = new ArrayList<>();
    @Builder.Default
    private List<String> misconceptions = new ArrayList<>();
    @Builder.Default
    private List<String> recommendedReview = new ArrayList<>();
    private String suggestedNextStep;
}
