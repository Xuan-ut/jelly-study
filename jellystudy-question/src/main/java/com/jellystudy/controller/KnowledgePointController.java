package com.jellystudy.controller;

import com.jellystudy.dubbo.KnowledgePointDubboService;
import com.jellystudy.dubbo.QuestionDubboService;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.entity.Question;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/knowledge-points")
public class KnowledgePointController {
    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.KnowledgePointDubboService", timeout = 30000, check = false)
    private KnowledgePointDubboService knowledgePointService;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.QuestionDubboService", timeout = 120000, check = false)
    private QuestionDubboService questionService;

    @PostMapping
    public KnowledgePoint create(@RequestBody KnowledgePoint knowledgePoint) {
        return knowledgePointService.create(knowledgePoint);
    }

    @GetMapping
    public List<KnowledgePoint> findAll() {
        return knowledgePointService.findAll();
    }

    @GetMapping("/{id}")
    public KnowledgePoint findById(@PathVariable String id) {
        return knowledgePointService.findById(id);
    }

    @PutMapping
    public KnowledgePoint update(@RequestBody KnowledgePoint knowledgePoint) {
        return knowledgePointService.update(knowledgePoint);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        knowledgePointService.delete(id);
    }

    @GetMapping("/parent/{parentId}")
    public List<KnowledgePoint> findByParentId(@PathVariable String parentId) {
        return knowledgePointService.findByParentId(parentId);
    }

    @GetMapping("/name/{name}")
    public KnowledgePoint findByName(@PathVariable String name) {
        return knowledgePointService.findByName(name);
    }

    @GetMapping("/count")
    public long count() {
        return knowledgePointService.count();
    }

    @GetMapping("/{id}/stats")
    public Map<String, Object> getKnowledgePointStats(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        
        KnowledgePoint kp = knowledgePointService.findById(id);
        if (kp == null) {
            result.put("success", false);
            result.put("message", "知识点不存在");
            return result;
        }
        
        result.put("knowledgePoint", kp);
        
        try {
            List<Question> questions = questionService.getQuestionsByKnowledgePoint(id);
            result.put("questionCount", questions.size());
            result.put("success", true);
            log.info("获取知识点[{}]统计: {}个相关问题", kp.getName(), questions.size());
        } catch (Exception e) {
            log.warn("调用QuestionService获取知识点统计失败: {}", e.getMessage());
            result.put("questionCount", 0);
            result.put("success", true);
            result.put("warning", "问题统计获取失败");
        }
        
        try {
            List<Question> hotQuestions = questionService.getHotQuestions();
            int hotCount = 0;
            for (Question q : hotQuestions) {
                if (q.getKnowledgePoints() != null && q.getKnowledgePoints().contains(kp.getName())) {
                    hotCount++;
                }
            }
            result.put("hotQuestionCount", hotCount);
            log.info("知识点[{}]有{}个热点问题", kp.getName(), hotCount);
        } catch (Exception e) {
            log.warn("调用QuestionService获取热点问题失败: {}", e.getMessage());
            result.put("hotQuestionCount", 0);
        }
        
        return result;
    }

    @GetMapping("/with-stats")
    public List<Map<String, Object>> findAllWithStats() {
        List<KnowledgePoint> knowledgePoints = knowledgePointService.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (KnowledgePoint kp : knowledgePoints) {
            Map<String, Object> kpMap = new HashMap<>();
            kpMap.put("id", kp.getId());
            kpMap.put("name", kp.getName());
            kpMap.put("description", kp.getDescription());
            kpMap.put("parentId", kp.getParentId());
            
            try {
                List<Question> questions = questionService.getQuestionsByKnowledgePoint(kp.getId());
                kpMap.put("questionCount", questions.size());
            } catch (Exception e) {
                log.warn("获取知识点[{}]问题数失败: {}", kp.getName(), e.getMessage());
                kpMap.put("questionCount", 0);
            }
            
            result.add(kpMap);
        }
        
        log.info("获取所有知识点(含统计)，共{}个", result.size());
        return result;
    }
}
