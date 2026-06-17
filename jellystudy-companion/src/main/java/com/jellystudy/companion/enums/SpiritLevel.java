package com.jellystudy.companion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum SpiritLevel {

    SEED(0, "种子", 0, "一颗发光的种子", List.of("basic_greeting")),
    SPROUT(1, "芽苗", 100, "长出两片叶子", List.of("basic_greeting", "daily_suggestion")),
    YOUNG_SPIRIT(2, "幼灵", 500, "出现小翅膀", List.of("basic_greeting", "daily_suggestion", "weak_point_alert")),
    ADULT_SPIRIT(3, "成灵", 2000, "翅膀变大，发光", List.of("basic_greeting", "daily_suggestion", "weak_point_alert", "feynman_teaching")),
    SPIRIT_MASTER(4, "精灵使", 5000, "出现光环", List.of("basic_greeting", "daily_suggestion", "weak_point_alert", "feynman_teaching", "timeline_prediction")),
    SAGE(5, "贤者", 15000, "全身金光，王冠", List.of("basic_greeting", "daily_suggestion", "weak_point_alert", "feynman_teaching", "timeline_prediction", "hive_insight"));

    private final int level;
    private final String displayName;
    private final int requiredExp;
    private final String appearanceDesc;
    private final List<String> skills;

    /**
     * 根据等级值获取枚举
     */
    public static SpiritLevel getByLevel(int level) {
        for (SpiritLevel sl : values()) {
            if (sl.level == level) return sl;
        }
        return SEED;
    }

    /**
     * 根据累计经验值计算当前等级
     */
    public static SpiritLevel getByExp(int exp) {
        SpiritLevel result = SEED;
        for (SpiritLevel sl : values()) {
            if (exp >= sl.requiredExp) {
                result = sl;
            }
        }
        return result;
    }
}
