package com.jellystudy.companion.service.timeline;

import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;

/**
 * 知识树服务接口
 */
public interface KnowledgeTreeService {

    /** 构建/刷新用户知识树 */
    KnowledgeTreeSnapshot buildTree(Long userId);

    /** 获取最新知识树快照 */
    KnowledgeTreeSnapshot getLatestSnapshot(Long userId);

    /** 获取知识树预测 */
    TimelineService.KnowledgeTreePrediction getTreePrediction(Long userId, int daysAhead);
}
