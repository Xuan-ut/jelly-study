package com.jellystudy.companion.service.hive;

import java.util.List;
import java.util.Map;

/**
 * 学习路径推荐服务接口
 */
public interface PathRecommendService {

    /** 为用户推荐学习路径 */
    List<Map<String, Object>> recommendPaths(Long userId, String subject);
}
