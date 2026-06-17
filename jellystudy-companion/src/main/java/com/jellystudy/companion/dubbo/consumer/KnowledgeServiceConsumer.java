package com.jellystudy.companion.dubbo.consumer;

import com.jellystudy.dubbo.KnowledgePointDubboService;
import com.jellystudy.entity.KnowledgePoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 知识点服务 Dubbo Consumer
 */
@Component
@Slf4j
public class KnowledgeServiceConsumer {

    @DubboReference(version = "1.0.0", check = false)
    private KnowledgePointDubboService knowledgePointDubboService;

    /**
     * 获取知识点详情
     */
    public KnowledgePoint getKnowledgeDetail(String knowledgeId) {
        try {
            return knowledgePointDubboService.findById(knowledgeId);
        } catch (Exception e) {
            log.error("调用知识点服务失败: knowledgeId={}, error={}", knowledgeId, e.getMessage());
            return null;
        }
    }

    /**
     * 根据名称查找知识点
     */
    public KnowledgePoint findByName(String name) {
        try {
            return knowledgePointDubboService.findByName(name);
        } catch (Exception e) {
            log.error("调用知识点服务失败: name={}, error={}", name, e.getMessage());
            return null;
        }
    }

    /**
     * 获取知识点总数
     */
    public long count() {
        try {
            return knowledgePointDubboService.count();
        } catch (Exception e) {
            log.error("调用知识点服务失败: count, error={}", e.getMessage());
            return 0;
        }
    }
}
