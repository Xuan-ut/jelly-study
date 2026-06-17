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
@Document(collection = "anomaly_record")
public class AnomalyRecord {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private LocalDate reportDate;

    @Builder.Default
    private List<DimensionResult> dimensions = new ArrayList<>();

    private String overallRisk;

    private String overallRiskLevel;

    private LocalDate created;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionResult {
        private String name;
        private double userValue;
        private String normalRange;
        /** NORMAL / WARNING / CRITICAL */
        private String status;
        private String recommendation;
    }
}
