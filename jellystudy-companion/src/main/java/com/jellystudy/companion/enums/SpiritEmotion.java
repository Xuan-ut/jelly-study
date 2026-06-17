package com.jellystudy.companion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpiritEmotion {

    EXCITED("兴奋", "完成阶段/连续打卡"),
    HAPPY("开心", "正常完成每日任务"),
    CALM("平静", "有学习但未完成任务"),
    HUNGRY("饥饿", "连续2天未学习"),
    SLEEPING("沉睡", "连续7天未学习");

    private final String displayName;
    private final String triggerCondition;
}
