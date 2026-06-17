package com.jellystudy.companion.entity;

import com.jellystudy.companion.enums.GrowthEventType;
import com.jellystudy.companion.enums.SpiritEmotion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "spirit_state")
public class SpiritState {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    private String name;

    private Integer level;

    private String levelName;

    private Integer experience;

    private Integer nextLevelExp;

    private Integer satiation;

    private SpiritEmotion emotion;

    /** 当前性格标识 */
    private String personalityKey;

    /** 当前性格提示词 */
    private String personalityPrompt;

    private Appearance appearance;

    /** 当前选择的外形等级（用于显示对应等级的精灵图片，不超过实际等级） */
    @Builder.Default
    private Integer appearanceLevel = 0;

    private List<String> skills;

    private Memory memory;

    @Builder.Default
    private List<GrowthLog> growthLog = new ArrayList<>();

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Appearance {
        private String body;
        private String wings;
        private String aura;
        private String crown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Memory {
        private LocalDateTime lastInteraction;
        @Builder.Default
        private List<String> recentTopics = new ArrayList<>();
        @Builder.Default
        private List<String> userPreferences = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthLog {
        private LocalDate date;
        private GrowthEventType event;
        private String detail;
        private Integer expGained;
    }
}
