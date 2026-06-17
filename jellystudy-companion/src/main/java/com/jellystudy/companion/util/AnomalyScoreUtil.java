package com.jellystudy.companion.util;

/**
 * 异常检测评分工具
 * 使用 Z-Score 方法判断个体偏离群体均值的程度
 */
public final class AnomalyScoreUtil {

    private AnomalyScoreUtil() {}

    /**
     * 计算 Z-Score
     * Z = (x - μ) / σ
     */
    public static double calculateZScore(double userValue, double populationMean, double populationStdDev) {
        if (populationStdDev <= 0) return 0;
        return (userValue - populationMean) / populationStdDev;
    }

    /**
     * 根据 Z-Score 分类异常等级
     * |Z| <= 1.0 → NORMAL
     * 1.0 < |Z| <= 2.0 → WARNING
     * |Z| > 2.0 → CRITICAL
     */
    public static String classifyAnomaly(double zScore) {
        double absZ = Math.abs(zScore);
        if (absZ <= 1.0) return "NORMAL";
        if (absZ <= 2.0) return "WARNING";
        return "CRITICAL";
    }

    /**
     * 直接判断用户值是否在正常范围内
     */
    public static AnomalyResult assess(double userValue, double populationMean,
                                       double populationStdDev, String dimensionName) {
        double zScore = calculateZScore(userValue, populationMean, populationStdDev);
        String status = classifyAnomaly(zScore);

        double lowerBound = populationMean - populationStdDev;
        double upperBound = populationMean + populationStdDev;
        String normalRange = String.format("%.1f - %.1f", lowerBound, upperBound);

        String recommendation;
        switch (status) {
            case "CRITICAL":
                recommendation = String.format("您的%s严重偏离正常范围，建议重点关注", dimensionName);
                break;
            case "WARNING":
                recommendation = String.format("您的%s略低于正常水平，需要留意", dimensionName);
                break;
            default:
                recommendation = String.format("您的%s处于正常范围，继续保持", dimensionName);
                break;
        }

        AnomalyResult result = new AnomalyResult();
        result.setName(dimensionName);
        result.setUserValue(userValue);
        result.setNormalRange(normalRange);
        result.setStatus(status);
        result.setRecommendation(recommendation);
        result.setZScore(zScore);
        return result;
    }

    public static class AnomalyResult {
        private String name;
        private double userValue;
        private String normalRange;
        private String status;
        private String recommendation;
        private double zScore;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getUserValue() { return userValue; }
        public void setUserValue(double userValue) { this.userValue = userValue; }
        public String getNormalRange() { return normalRange; }
        public void setNormalRange(String normalRange) { this.normalRange = normalRange; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
        public double getZScore() { return zScore; }
        public void setZScore(double zScore) { this.zScore = zScore; }
    }
}
