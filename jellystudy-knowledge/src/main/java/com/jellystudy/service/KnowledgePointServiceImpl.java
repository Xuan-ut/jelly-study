package com.jellystudy.service;

import com.jellystudy.dubbo.KnowledgePointDubboService;
import com.jellystudy.dubbo.QuestionDubboService;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.repository.KnowledgePointRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.KnowledgePointDubboService")
public class KnowledgePointServiceImpl implements KnowledgePointDubboService {

    @Autowired
    private KnowledgePointRepository repository;
    
    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.QuestionDubboService", check = false)
    private QuestionDubboService questionService;

    @Override
    public KnowledgePoint create(KnowledgePoint knowledgePoint) {
        return repository.save(knowledgePoint);
    }

    @Override
    public KnowledgePoint findById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<KnowledgePoint> findAll() {
        return repository.findAll();
    }

    @Override
    public KnowledgePoint update(KnowledgePoint knowledgePoint) {
        return repository.save(knowledgePoint);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public List<KnowledgePoint> findByParentId(String parentId) {
        return repository.findByParentId(parentId);
    }

    @Override
    public KnowledgePoint findByName(String name) {
        return repository.findByName(name);
    }

    @Override
    public long count() {
        return repository.count();
    }
    
    /**
     * 获取知识点统计信息（调用 QuestionService 获取相关问题数量）
     */
    public KnowledgePointStatistics getStatistics(String knowledgePointId) {
        KnowledgePoint kp = findById(knowledgePointId);
        if (kp == null) {
            return null;
        }
        
        KnowledgePointStatistics stats = new KnowledgePointStatistics();
        stats.setKnowledgePointId(kp.getId());
        stats.setKnowledgePointName(kp.getName());
        stats.setQuestionCount(0);
        stats.setHotQuestionCount(0);
        
        try {
            // 调用问题服务获取相关问题数量
            long questionCount = questionService.getQuestionsByKnowledgePoint(knowledgePointId).size();
            stats.setQuestionCount((int) questionCount);
            
            // 统计热点问题数量
            List<com.jellystudy.entity.Question> hotQuestions = questionService.getHotQuestions();
            int hotCount = 0;
            for (com.jellystudy.entity.Question q : hotQuestions) {
                if (q.getKnowledgePoints() != null && q.getKnowledgePoints().contains(kp.getName())) {
                    hotCount++;
                }
            }
            stats.setHotQuestionCount(hotCount);
            
            log.info("获取知识点[{}]统计信息: {} 个问题, {} 个热点问题", 
                    kp.getName(), questionCount, hotCount);
        } catch (Exception e) {
            log.warn("获取知识点统计信息失败: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 内部类：知识点统计信息
     */
    public static class KnowledgePointStatistics {
        private String knowledgePointId;
        private String knowledgePointName;
        private int questionCount;
        private int hotQuestionCount;
        
        // Getters and Setters
        public String getKnowledgePointId() { return knowledgePointId; }
        public void setKnowledgePointId(String knowledgePointId) { this.knowledgePointId = knowledgePointId; }
        public String getKnowledgePointName() { return knowledgePointName; }
        public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }
        public int getQuestionCount() { return questionCount; }
        public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
        public int getHotQuestionCount() { return hotQuestionCount; }
        public void setHotQuestionCount(int hotQuestionCount) { this.hotQuestionCount = hotQuestionCount; }
    }
}
