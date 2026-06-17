package com.jellystudy.companion.util;

/**
 * 遗忘曲线计算工具
 * 基于艾宾浩斯遗忘曲线 R = e^(-t/S)
 */
public final class ForgettingCurveUtil {

    private ForgettingCurveUtil() {}

    /**
     * 计算保留率
     * R = e^(-t/S)
     *
     * @param daysSinceLastReview 距上次复习的天数
     * @param stability           稳定性系数（与掌握度正相关）
     * @return 保留率 0.0-1.0
     */
    public static double calculateRetention(int daysSinceLastReview, double stability) {
        if (daysSinceLastReview <= 0) return 1.0;
        if (stability <= 0) stability = 1;
        return Math.exp(-daysSinceLastReview / stability);
    }

    /**
     * 根据掌握度计算稳定性系数
     * 掌握度90% → S=50
     * 掌握度60% → S=20
     * 掌握度30% → S=5
     */
    public static double calculateStability(double mastery) {
        if (mastery <= 0) return 1;
        if (mastery >= 1.0) return 100;
        // 指数映射: mastery 0.0->1.0 映射到 stability 1->100
        return Math.exp(mastery * 4.6); // e^4.6 ≈ 100
    }

    /**
     * 预测N天后的保留率
     */
    public static double predictRetention(double mastery, int daysSinceLastReview, int daysAhead) {
        double stability = calculateStability(mastery);
        double currentRetention = calculateRetention(daysSinceLastReview, stability);
        return calculateRetention(daysAhead, stability);
    }
}
