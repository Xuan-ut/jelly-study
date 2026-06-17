package com.jellystudy.companion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrowthEventType {

    TASK_COMPLETED("完成任务", "📝"),
    STAGE_COMPLETED("完成阶段", "🏆"),
    PLAN_COMPLETED("完成计划", "🎉"),
    DAILY_STREAK("连续打卡", "🔥"),
    LEVEL_UP("升级", "⬆️"),
    SKILL_UNLOCKED("解锁技能", "✨"),
    MILESTONE_REACHED("里程碑达成", "🎯"),
    AWAKENED("被唤醒", "💤"),
    MANUAL_FEED("手动喂养", "🍎");

    private final String description;
    private final String icon;
}
