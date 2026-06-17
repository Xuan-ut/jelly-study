package com.jellystudy.controller;

import com.jellystudy.config.KnowledgeConfig;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.repository.KnowledgePointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-points")
public class KnowledgePointController {

    @Autowired
    private KnowledgePointRepository repository;

    @Autowired
    private KnowledgeConfig knowledgeConfig;

    @Value("${jellystudy.knowledge.default-page-size:10}")
    private int defaultPageSize;

    @Value("${jellystudy.knowledge.max-name-length:50}")
    private int maxNameLength;

    @Value("${jellystudy.knowledge.welcome-message:欢迎使用JellyStudy知识点管理}")
    private String welcomeMessage;

    @GetMapping("/nacos-config")
    public Map<String, Object> getNacosConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("source", "Nacos Config Center");
        config.put("defaultPageSize", knowledgeConfig.getDefaultPageSize());
        config.put("maxNameLength", knowledgeConfig.getMaxNameLength());
        config.put("autoCreateEnabled", knowledgeConfig.isAutoCreateEnabled());
        config.put("welcomeMessage", knowledgeConfig.getWelcomeMessage());
        config.put("defaultPageSizeByValue", defaultPageSize);
        config.put("maxNameLengthByValue", maxNameLength);
        config.put("welcomeMessageByValue", welcomeMessage);
        config.put("timestamp", System.currentTimeMillis());
        return config;
    }

    @PostMapping
    public KnowledgePoint create(@RequestBody KnowledgePoint knowledgePoint) {
        if (knowledgePoint.getName() != null && knowledgePoint.getName().length() > knowledgeConfig.getMaxNameLength()) {
            throw new IllegalArgumentException("知识点名称长度不能超过 " + knowledgeConfig.getMaxNameLength() + " 个字符");
        }
        return repository.save(knowledgePoint);
    }

    @GetMapping
    public List<KnowledgePoint> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public KnowledgePoint findById(@PathVariable String id) {
        return repository.findById(id).orElse(null);
    }

    @PutMapping
    public KnowledgePoint update(@RequestBody KnowledgePoint knowledgePoint) {
        return repository.save(knowledgePoint);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repository.deleteById(id);
    }

    @GetMapping("/parent/{parentId}")
    public List<KnowledgePoint> findByParentId(@PathVariable String parentId) {
        return repository.findByParentId(parentId);
    }

    @GetMapping("/name/{name}")
    public KnowledgePoint findByName(@PathVariable String name) {
        return repository.findByName(name);
    }
}
