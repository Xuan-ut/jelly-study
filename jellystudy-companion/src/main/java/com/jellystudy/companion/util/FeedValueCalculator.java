package com.jellystudy.companion.util;

import com.jellystudy.companion.config.SpiritConfigProperties;

/**
 * 喂养值计算器
 * 根据事件类型映射喂养值
 */
public final class FeedValueCalculator {

    private FeedValueCalculator() {}

    /**
     * 根据事件类型计算喂养值
     */
    public static int calculate(String eventType, SpiritConfigProperties config) {
        if (eventType == null) return 0;
        switch (eventType) {
            case "task.completed":
                return config.getFeedTaskComplete();
            case "stage.completed":
                return config.getFeedStageComplete();
            case "plan.completed":
                return config.getFeedStreak7();
            case "manual_feed":
                return config.getFeedTaskComplete();
            default:
                // 尝试匹配包含关键词的事件类型（如MQ routing key）
                if (eventType.contains("task")) return config.getFeedTaskComplete();
                if (eventType.contains("stage")) return config.getFeedStageComplete();
                if (eventType.contains("plan")) return config.getFeedStreak7();
                if (eventType.contains("daily")) return config.getFeedStreak7();
                return 0;
        }
    }

    /**
     * 计算连续打卡额外奖励
     */
    public static int calculateStreakBonus(int streakDays, SpiritConfigProperties config) {
        if (streakDays >= 7) {
            return config.getFeedStreak7();
        }
        return 0;
    }

    /**
     * 计算饥饿扣除值（每天未学习扣多少）
     */
    public static int calculateHungerDeduction(int inactiveDays, SpiritConfigProperties config) {
        return inactiveDays * config.getHungerPerDay();
    }
}
