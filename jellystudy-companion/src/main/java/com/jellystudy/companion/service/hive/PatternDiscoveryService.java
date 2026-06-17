package com.jellystudy.companion.service.hive;

import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.enums.HivePatternType;

import java.util.List;

/**
 * 群体模式发现服务接口
 */
public interface PatternDiscoveryService {

    /** 发现新的学习模式（由每日定时任务调用） */
    void discoverPatterns();

    /** 按类型查询模式 */
    List<HivePattern> getPatternsByType(HivePatternType type);

    /** 按学科查询模式 */
    List<HivePattern> getPatternsBySubject(String subject);
}
