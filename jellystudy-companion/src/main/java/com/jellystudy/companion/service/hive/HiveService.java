package com.jellystudy.companion.service.hive;

import com.jellystudy.companion.entity.AnomalyRecord;
import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.entity.LearningEvent;

import java.util.List;

/**
 * 群体智慧蜂巢主服务接口
 */
public interface HiveService {

    /** 收集学习数据（MQ消费） */
    void collectLearningData(LearningEvent event);

    /** 获取指定学科的群体模式 */
    List<HivePattern> getPatterns(String subject);

    /** 获取个体异常检测报告 */
    AnomalyRecord getAnomalyReport(Long userId);
}
