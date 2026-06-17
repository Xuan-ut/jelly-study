package com.jellystudy.companion.entity;

import com.jellystudy.companion.enums.HivePatternType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "hive_pattern")
public class HivePattern {

    @Id
    private String id;

    @Indexed(unique = true)
    private String patternId;

    private HivePatternType type;

    private String subject;

    private String knowledgeId;

    private String description;

    @Builder.Default
    private Map<String, Object> statistics = new HashMap<>();

    @Builder.Default
    private List<Intervention> interventions = new ArrayList<>();

    private String discoveredAt;

    private double confidence;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Intervention {
        private String trigger;
        private String action;
        private double effectiveness;
    }
}
