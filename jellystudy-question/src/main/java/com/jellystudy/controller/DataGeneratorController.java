package com.jellystudy.controller;

import com.jellystudy.service.DataGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataGeneratorController {

    @Autowired
    private DataGeneratorService dataGeneratorService;

    @GetMapping("/generate")
    public Map<String, Object> generateData() {
        dataGeneratorService.generateRandomData();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "随机数据生成完成");
        result.put("questionCount", dataGeneratorService.getQuestionCount());
        result.put("knowledgePointCount", dataGeneratorService.getKnowledgePointCount());
        return result;
    }
}
