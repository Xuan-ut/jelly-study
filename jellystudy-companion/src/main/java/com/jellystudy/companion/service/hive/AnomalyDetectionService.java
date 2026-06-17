package com.jellystudy.companion.service.hive;

import com.jellystudy.companion.entity.AnomalyRecord;

/**
 * 个体异常检测服务接口
 */
public interface AnomalyDetectionService {

    /** 检测用户的学习异常 */
    AnomalyRecord detectAnomalies(Long userId);

    /** 生成异常报告 */
    AnomalyRecord generateReport(Long userId);
}
