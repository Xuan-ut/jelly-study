package com.jellystudy.companion.service.hive;

import java.util.Map;

/**
 * 学习健康报告服务接口
 */
public interface HealthReportService {

    /** 生成用户学习健康报告 */
    Map<String, Object> generateHealthReport(Long userId);
}
