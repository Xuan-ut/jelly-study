package com.jellystudy.companion.service.timeline;

/**
 * 遗忘曲线服务接口
 */
public interface ForgettingCurveService {

    /** 计算用户所有知识点的当前保留率 */
    void calculateAllRetentions(Long userId);

    /** 计算单个知识点的保留率 */
    double calculateNodeRetention(double mastery, int daysSinceLastReview);
}
