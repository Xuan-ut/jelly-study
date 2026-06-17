package com.jellystudy.companion.service.timeline;

import java.util.List;

/**
 * 预警服务接口
 */
public interface EarlyWarningService {

    /** 检查用户当前的关键节点预警 */
    List<TimelineService.EarlyWarning> checkWarnings(Long userId);
}
