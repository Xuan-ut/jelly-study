package com.jellystudy.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.dubbo.AIDubboService;
import com.jellystudy.entity.StudyPlan;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.AIDubboService", timeout = 120000, check = false)
    private AIDubboService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/generate-plan")
    public Map<String, Object> generatePlan(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String subject = (String) request.getOrDefault("subject", "");
            String goal = (String) request.getOrDefault("goal", "");
            String difficulty = (String) request.getOrDefault("difficulty", "中级");

            String planDetail = aiService.generatePlanDetail(subject, goal, 0, difficulty);
            result.put("success", true);
            result.put("text", planDetail);

            StudyPlan plan = new StudyPlan();
            plan.setTitle(subject + "学习计划");
            plan.setDescription(goal != null && !goal.isEmpty() ? "学习目标: " + goal : "AI智能生成的学习计划");

            List<StudyPlan.PlanStage> stages = parseAIStages(planDetail, 0, subject);
            plan.setStages(stages);
            result.put("plan", plan);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private List<StudyPlan.PlanStage> parseAIStages(String aiResponse, int defaultCount, String subject) {
        List<StudyPlan.PlanStage> stages = new ArrayList<>();
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return buildDefaultStages(defaultCount, subject);
        }

        String json = extractJSON(aiResponse);
        if (json != null) {
            try {
                List<Map<String, Object>> stageList = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
                for (int i = 0; i < stageList.size(); i++) {
                    Map<String, Object> s = stageList.get(i);
                    StudyPlan.PlanStage stage = buildStageFromMap(s, i);
                    stages.add(stage);
                }
                if (!stages.isEmpty()) {
                    return stages;
                }
            } catch (Exception e) {
                List<Map<String, Object>> repaired = tryRepairAndParseJSON(json);
                if (repaired != null) {
                    for (int i = 0; i < repaired.size(); i++) {
                        Map<String, Object> s = repaired.get(i);
                        StudyPlan.PlanStage stage = buildStageFromMap(s, i);
                        stages.add(stage);
                    }
                    if (!stages.isEmpty()) {
                        return stages;
                    }
                }
            }
        }

        List<StudyPlan.PlanStage> textStages = parseTextResponse(aiResponse, defaultCount);
        if (!textStages.isEmpty()) {
            return textStages;
        }

        return buildDefaultStages(defaultCount, subject);
    }

    private List<Map<String, Object>> tryRepairAndParseJSON(String json) {
        try {
            String repaired = json.trim();
            if (!repaired.endsWith("]")) {
                int lastBrace = repaired.lastIndexOf('}');
                if (lastBrace > 0) {
                    repaired = repaired.substring(0, lastBrace + 1) + "]";
                }
            }
            if (!repaired.startsWith("[")) {
                repaired = "[" + repaired;
            }
            return objectMapper.readValue(repaired, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            try {
                String[] objects = json.split("(?<=\\})\\s*,\\s*(?=\\{)");
                List<Map<String, Object>> result = new ArrayList<>();
                for (String obj : objects) {
                    String trimmed = obj.trim();
                    if (trimmed.startsWith("{")) {
                        try {
                            Map<String, Object> map = objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
                            result.add(map);
                        } catch (Exception ignored) {}
                    }
                }
                if (!result.isEmpty()) return result;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractJSON(String text) {
        String trimmed = text.trim();

        Pattern mdPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```", Pattern.CASE_INSENSITIVE);
        Matcher mdMatcher = mdPattern.matcher(trimmed);
        if (mdMatcher.find()) {
            String extracted = mdMatcher.group(1).trim();
            if (extracted.startsWith("[") || extracted.startsWith("{")) {
                return extracted;
            }
        }

        Pattern mdOpenPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*)", Pattern.CASE_INSENSITIVE);
        Matcher mdOpenMatcher = mdOpenPattern.matcher(trimmed);
        if (mdOpenMatcher.find()) {
            String extracted = mdOpenMatcher.group(1).trim();
            if (extracted.startsWith("[") || extracted.startsWith("{")) {
                return extracted;
            }
        }

        int startIdx = trimmed.indexOf('[');
        int endIdx = trimmed.lastIndexOf(']');
        if (startIdx >= 0 && endIdx > startIdx) {
            return trimmed.substring(startIdx, endIdx + 1);
        }

        int objStart = trimmed.indexOf('{');
        int objEnd = trimmed.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            return "[" + trimmed.substring(objStart, objEnd + 1) + "]";
        }

        return null;
    }

    private StudyPlan.PlanStage buildStageFromMap(Map<String, Object> s, int index) {
        StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
        String name = getStringValue(s, "name", "");
        if (name.isEmpty()) {
            name = getStringValue(s, "title", "");
        }
        if (name.isEmpty()) {
            name = getStringValue(s, "stageName", "");
        }
        if (name.isEmpty()) {
            name = "阶段" + (index + 1);
        }
        stage.setName(name);

        StringBuilder desc = new StringBuilder();
        String description = getStringValue(s, "description", "");
        if (description.isEmpty()) {
            description = getStringValue(s, "desc", "");
        }
        if (description.isEmpty()) {
            description = getStringValue(s, "content", "");
        }
        if (!description.isEmpty()) {
            desc.append(description);
        }

        Object methods = s.get("learningMethods");
        if (methods != null && !methods.toString().isEmpty()) {
            if (desc.length() > 0) desc.append("\n\n📖 学习方法: ");
            else desc.append("📖 学习方法: ");
            desc.append(methods.toString());
        }

        Object hours = s.get("estimatedHours");
        if (hours == null) hours = s.get("hours");
        if (hours != null) {
            try {
                double h = Double.parseDouble(hours.toString());
                stage.setEstimatedHours(h);
                desc.append("\n⏱ 预计时长: ").append(h).append("小时");
            } catch (NumberFormatException ignored) {
            }
        }

        Object topics = s.get("keyTopics");
        if (topics == null) topics = s.get("topics");
        if (topics == null) topics = s.get("knowledgePoints");
        if (topics instanceof List) {
            List<String> topicList = new ArrayList<>();
            for (Object t : (List<?>) topics) {
                topicList.add(t.toString());
            }
            if (!topicList.isEmpty()) {
                desc.append("\n📌 核心知识点: ").append(String.join("、", topicList));
            }
        }

        List<String> tasks = new ArrayList<>();
        Object milestones = s.get("milestones");
        if (milestones instanceof List) {
            for (Object m : (List<?>) milestones) {
                tasks.add(m.toString());
            }
        }
        Object taskObj = s.get("tasks");
        if (taskObj instanceof List) {
            for (Object t : (List<?>) taskObj) {
                tasks.add(t.toString());
            }
        }
        stage.setTasks(tasks.isEmpty() ? null : tasks);

        stage.setDescription(desc.toString());
        stage.setProgress(0);
        stage.setOrder(index);
        return stage;
    }

    private List<StudyPlan.PlanStage> parseTextResponse(String text, int defaultCount) {
        List<StudyPlan.PlanStage> stages = new ArrayList<>();
        String[] lines = text.split("\n");

        Pattern stagePattern = Pattern.compile("^\\s*(?:\\d+[.、)）]\\s*|阶段\\s*\\d+\\s*[:：]?\\s*|第\\s*\\d+\\s*阶段\\s*[:：]?\\s*)(.+)");
        String currentName = null;
        StringBuilder currentDesc = new StringBuilder();

        for (String line : lines) {
            Matcher matcher = stagePattern.matcher(line);
            if (matcher.find()) {
                if (currentName != null) {
                    StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
                    stage.setName(currentName);
                    stage.setDescription(currentDesc.toString().trim());
                    stage.setProgress(0);
                    stage.setOrder(stages.size());
                    stages.add(stage);
                }
                currentName = matcher.group(1).trim();
                currentDesc = new StringBuilder();
            } else if (currentName != null) {
                if (currentDesc.length() > 0) currentDesc.append("\n");
                currentDesc.append(line.trim());
            }
        }

        if (currentName != null) {
            StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
            stage.setName(currentName);
            stage.setDescription(currentDesc.toString().trim());
            stage.setProgress(0);
            stage.setOrder(stages.size());
            stages.add(stage);
        }

        if (stages.isEmpty() && text.length() > 50) {
            StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
            stage.setName("学习计划");
            stage.setDescription(text.trim());
            stage.setProgress(0);
            stage.setOrder(0);
            stages.add(stage);
        }

        return stages;
    }

    private List<StudyPlan.PlanStage> buildDefaultStages(int count, String subject) {
        if (count <= 0) count = 3;
        List<StudyPlan.PlanStage> stages = new ArrayList<>();
        String[] defaultNames = {"基础入门", "核心学习", "实践应用", "进阶提升", "总结巩固", "拓展深化"};
        for (int i = 0; i < count && i < defaultNames.length; i++) {
            StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
            stage.setName(defaultNames[i]);
            stage.setDescription(subject + "学习 - " + defaultNames[i] + "阶段");
            stage.setProgress(0);
            stage.setOrder(i);
            stages.add(stage);
        }
        if (stages.isEmpty()) {
            StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
            stage.setName("学习阶段");
            stage.setDescription(subject + "学习计划");
            stage.setProgress(0);
            stage.setOrder(0);
            stages.add(stage);
        }
        return stages;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = request.getOrDefault("message", "");
            String response = aiService.chat(message);
            result.put("success", true);
            result.put("response", response);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
