package com.jellystudy.companion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpiritSkill {

    BASIC_GREETING("basic_greeting", "基础问候"),
    DAILY_SUGGESTION("daily_suggestion", "每日建议"),
    WEAK_POINT_ALERT("weak_point_alert", "薄弱点提醒"),
    FEYNMAN_TEACHING("feynman_teaching", "费曼反转教学"),
    TIMELINE_PREDICTION("timeline_prediction", "时空预测"),
    HIVE_INSIGHT("hive_insight", "群体智慧洞察");

    private final String code;
    private final String description;
}
