package com.jellystudy.companion.service.hive;

import com.jellystudy.companion.ai.AIClient;
import com.jellystudy.companion.ai.prompt.HivePromptBuilder;
import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.enums.HivePatternType;
import com.jellystudy.companion.repository.HivePatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习路径推荐服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PathRecommendServiceImpl implements PathRecommendService {

    private final HivePatternRepository patternRepository;
    private final AIClient aiClient;
    private final HivePromptBuilder promptBuilder;

    @Override
    public List<Map<String, Object>> recommendPaths(Long userId, String subject) {
        // 查询该学科下已发现的路径模式
        List<HivePattern> patterns = patternRepository.findBySubject(subject).stream()
                .filter(p -> p.getType() == HivePatternType.PATH)
                .filter(p -> p.getConfidence() >= 0.7)
                .collect(Collectors.toList());

        List<Map<String, Object>> paths = new ArrayList<>();
        for (HivePattern pattern : patterns) {
            Map<String, Object> path = new LinkedHashMap<>();
            path.put("pathId", pattern.getPatternId());
            path.put("name", pattern.getDescription());
            path.put("successRate", pattern.getStatistics().getOrDefault("successRate", 0.7));
            path.put("avgDays", pattern.getStatistics().getOrDefault("avgDays", 14));
            if (pattern.getInterventions() != null && !pattern.getInterventions().isEmpty()) {
                path.put("suitableFor", pattern.getInterventions().get(0).getAction());
            }
            paths.add(path);
        }

        // 如果没有足够的路径模式，构建默认路径
        if (paths.isEmpty()) {
            Map<String, Object> defaultPath = new LinkedHashMap<>();
            defaultPath.put("pathId", UUID.randomUUID().toString().substring(0, 8));
            defaultPath.put("name", "标准学习路径");
            defaultPath.put("steps", List.of("基础知识 → 进阶概念 → 实战练习"));
            defaultPath.put("successRate", 0.85);
            defaultPath.put("avgDays", 14);
            defaultPath.put("suitableFor", "系统性学习者");
            defaultPath.put("recommended", true);
            paths.add(defaultPath);
        }

        return paths;
    }
}
