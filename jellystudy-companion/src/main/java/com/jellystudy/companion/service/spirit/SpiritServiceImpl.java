package com.jellystudy.companion.service.spirit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.companion.ai.AIClient;
import com.jellystudy.companion.config.CacheConfigProperties;
import com.jellystudy.companion.constant.RedisKeyConstants;
import com.jellystudy.companion.dubbo.consumer.AIServiceConsumer;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.enums.SpiritEmotion;
import com.jellystudy.companion.repository.SpiritStateRepository;
import com.jellystudy.dubbo.StudyPlanDubboService;
import com.jellystudy.entity.DailyTaskBoard;
import com.jellystudy.entity.StudyPlan;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 精灵系统主服务
 * 缓存优先模式: Redis → MongoDB → 回填缓存
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpiritServiceImpl implements SpiritService {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final SpiritStateRepository spiritStateRepository;
    private final StringRedisTemplate redisTemplate;
    private final SpiritGrowService spiritGrowService;
    private final SpiritEmotionService emotionService;
    private final SpiritChatService chatService;
    private final CacheConfigProperties cacheConfig;
    private final AIClient aiClient;
    private final AIServiceConsumer aiServiceConsumer;

    @DubboReference(version = "1.0.0", check = false)
    private StudyPlanDubboService studyPlanDubboService;

    @Override
    @SneakyThrows
    public SpiritState getSpiritState(Long userId) {
        String key = RedisKeyConstants.spiritState(userId);
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            log.debug("精灵状态命中缓存: userId={}", userId);
            return objectMapper.readValue(cached, SpiritState.class);
        }
        SpiritState state = spiritStateRepository.findByUserId(userId)
                .orElseGet(() -> spiritGrowService.initSpiritState(userId));
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(state),
                cacheConfig.getSpiritTtl(), TimeUnit.MINUTES);
        return state;
    }

    @Override
    public SpiritGreetingResult getGreeting(Long userId) {
        SpiritState spirit = getSpiritState(userId);
        SpiritEmotion currentEmotion = emotionService.calculateEmotion(userId);
        // 如果计算出的情感与缓存不一致，更新到数据库并刷新缓存
        if (spirit.getEmotion() != currentEmotion) {
            spirit.setEmotion(currentEmotion);
            spiritStateRepository.save(spirit);
            String key = RedisKeyConstants.spiritState(userId);
            redisTemplate.delete(key);
            spirit = getSpiritState(userId);
        }
        return chatService.getGreeting(spirit);
    }

    @Override
    public SpiritChatResult chat(Long userId, String sessionId, String message) {
        SpiritState spirit = getSpiritState(userId);
        return chatService.chat(spirit, sessionId, message);
    }

    @Override
    public SpiritState feed(Long userId, String eventType, int feedValue) {
        SpiritState state = spiritGrowService.feedSpirit(userId, eventType, feedValue);
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        spiritGrowService.checkAndTriggerEvolution(userId);
        return getSpiritState(userId);
    }

    @Override
    public SpiritState renameSpirit(Long userId, String newName) {
        SpiritState spirit = getSpiritState(userId);
        spirit.setName(newName);
        spirit.setUpdateTime(java.time.LocalDateTime.now());
        spiritStateRepository.save(spirit);
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        return getSpiritState(userId);
    }

    @Override
    public SpiritState switchPersonality(Long userId, String personalityKey, String personalityPrompt) {
        SpiritState spirit = getSpiritState(userId);
        spirit.setPersonalityKey(personalityKey);
        spirit.setPersonalityPrompt(personalityPrompt);
        spirit.setUpdateTime(java.time.LocalDateTime.now());
        spiritStateRepository.save(spirit);
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        log.info("精灵性格切换: userId={}, personalityKey={}", userId, personalityKey);
        return getSpiritState(userId);
    }

    @Override
    public SpiritChatResult proactiveChat(Long userId) {
        SpiritState spirit = getSpiritState(userId);
        return chatService.proactiveChat(spirit);
    }

    @Override
    public SpiritState switchAppearance(Long userId, int targetLevel) {
        SpiritState spirit = getSpiritState(userId);
        int currentLevel = spirit.getLevel() != null ? spirit.getLevel() : 0;
        // 只能选择当前等级及以下的外形
        if (targetLevel < 0 || targetLevel > currentLevel) {
            throw new IllegalArgumentException("只能选择已解锁等级的外形，当前等级: " + currentLevel);
        }
        com.jellystudy.companion.enums.SpiritLevel targetSpiritLevel =
                com.jellystudy.companion.enums.SpiritLevel.getByLevel(targetLevel);
        SpiritState.Appearance newAppearance = SpiritState.Appearance.builder()
                .body(targetSpiritLevel.getAppearanceDesc())
                .build();
        // 根据等级设置装饰
        switch (targetSpiritLevel) {
            case SPROUT:
                newAppearance.setWings(null); break;
            case YOUNG_SPIRIT:
                newAppearance.setWings("small"); break;
            case ADULT_SPIRIT:
                newAppearance.setWings("large_glowing"); break;
            case SPIRIT_MASTER:
                newAppearance.setWings("large_glowing");
                newAppearance.setAura("halo"); break;
            case SAGE:
                newAppearance.setWings("large_glowing");
                newAppearance.setAura("golden");
                newAppearance.setCrown("crown"); break;
            default: break;
        }
        spirit.setAppearance(newAppearance);
        spirit.setAppearanceLevel(targetLevel);
        spirit.setUpdateTime(java.time.LocalDateTime.now());
        spiritStateRepository.save(spirit);
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        log.info("精灵外形切换: userId={}, targetLevel={}", userId, targetLevel);
        return getSpiritState(userId);
    }

    @Override
    public TodayTasksResult getTodayTasks(Long userId) {
        String today = LocalDate.now().toString();

        // 获取今日每日任务
        List<Map<String, Object>> taskList = new ArrayList<>();
        int completedCount = 0;
        try {
            DailyTaskBoard board = studyPlanDubboService.getDailyTaskBoard(userId, today);
            if (board != null && board.getTasks() != null) {
                for (DailyTaskBoard.DailyTask task : board.getTasks()) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("taskId", task.getTaskId());
                    t.put("content", task.getContent());
                    t.put("completed", task.isCompleted());
                    t.put("planId", task.getPlanId());
                    t.put("stageId", task.getStageId());
                    t.put("order", task.getOrder());
                    taskList.add(t);
                    if (task.isCompleted()) completedCount++;
                }
            }
        } catch (Exception e) {
            log.warn("获取今日任务失败: userId={}, error={}", userId, e.getMessage());
        }

        // 获取用户活跃学习计划
        List<Map<String, Object>> planList = new ArrayList<>();
        try {
            List<StudyPlan> plans = studyPlanDubboService.findByUserId(userId);
            if (plans != null) {
                for (StudyPlan plan : plans) {
                    if ("COMPLETED".equals(plan.getStatus())) continue;
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("planId", plan.getId());
                    p.put("title", plan.getTitle());
                    p.put("progress", plan.getTotalProgress());
                    p.put("status", plan.getStatus());
                    int totalStages = plan.getStages() != null ? plan.getStages().size() : 0;
                    int doneStages = 0;
                    if (plan.getStages() != null) {
                        for (StudyPlan.PlanStage s : plan.getStages()) {
                            if ("COMPLETED".equals(s.getStatus()) || s.getProgress() >= 100) doneStages++;
                        }
                    }
                    p.put("totalStages", totalStages);
                    p.put("completedStages", doneStages);
                    // 当前阶段信息
                    String currentStage = "";
                    if (plan.getStages() != null) {
                        for (StudyPlan.PlanStage s : plan.getStages()) {
                            if (!"COMPLETED".equals(s.getStatus()) && s.getProgress() < 100) {
                                currentStage = s.getName();
                                break;
                            }
                        }
                    }
                    p.put("currentStage", currentStage);
                    planList.add(p);
                }
            }
        } catch (Exception e) {
            log.warn("获取学习计划失败: userId={}, error={}", userId, e.getMessage());
        }

        return new TodayTasksResult(planList, taskList, completedCount, taskList.size());
    }

    @Override
    public TaskToggleResult toggleTask(Long userId, String taskId, String date) {
        String targetDate = (date != null && !date.isEmpty()) ? date : LocalDate.now().toString();
        try {
            DailyTaskBoard board = studyPlanDubboService.getDailyTaskBoard(userId, targetDate);
            if (board == null) {
                board = new DailyTaskBoard();
                board.setUserId(userId);
                board.setDate(targetDate);
                board.setTasks(new ArrayList<>());
            }
            if (board.getTasks() == null) board.setTasks(new ArrayList<>());

            boolean nowCompleted = false;
            String taskContent = "";
            for (DailyTaskBoard.DailyTask task : board.getTasks()) {
                if (taskId.equals(task.getTaskId())) {
                    task.setCompleted(!task.isCompleted());
                    nowCompleted = task.isCompleted();
                    taskContent = task.getContent();
                    break;
                }
            }

            studyPlanDubboService.saveDailyTaskBoard(board);

            String encouragement = "";
            String emotion = "CALM";
            if (nowCompleted) {
                // 完成任务：喂养精灵 + AI鼓励
                SpiritState state = feed(userId, "task_completed", 10);
                emotion = SpiritEmotion.HAPPY.name();

                String personalityPrompt = state.getPersonalityPrompt() != null
                        ? state.getPersonalityPrompt()
                        : "你是一个名叫小光的AI学习精灵，性格好奇、温暖、偶尔调皮。";
                String prompt = String.format(
                        "%s\n\n用户刚完成了一个学习任务：「%s」。\n" +
                        "请你给用户一句简短的鼓励回应（20字以内），要符合你的性格，热情自然。",
                        personalityPrompt, taskContent
                );
                try {
                    encouragement = aiClient.chat("精灵鼓励用户完成任务", prompt);
                } catch (Exception e) {
                    encouragement = "太棒了！完成任务的感觉真好！继续保持！";
                }
            } else {
                encouragement = "好的，已取消完成状态，继续加油哦！";
                emotion = "CALM";
            }

            return new TaskToggleResult(nowCompleted, encouragement, emotion);
        } catch (Exception e) {
            log.error("切换任务状态失败: userId={}, taskId={}, error={}", userId, taskId, e.getMessage());
            return new TaskToggleResult(false, "操作失败，请稍后再试", "CALM");
        }
    }

    @Override
    public QuickPlanResult quickGeneratePlan(Long userId, String topic) {
        try {
            // 优先使用学习计划服务的AI接口生成计划
            String aiPlanJson = aiServiceConsumer.generatePlanDetail(topic, "全面掌握" + topic, 3, "中等");

            List<Map<String, Object>> stageMaps = null;
            if (aiPlanJson != null && !aiPlanJson.isEmpty()) {
                stageMaps = parseStagesJson(aiPlanJson);
            }

            // 如果AI服务调用失败，使用本地AI兜底
            if (stageMaps == null || stageMaps.isEmpty()) {
                SpiritState spirit = getSpiritState(userId);
                String personalityPrompt = spirit.getPersonalityPrompt() != null
                        ? spirit.getPersonalityPrompt()
                        : "你是一个名叫小光的AI学习精灵。";
                String prompt = String.format(
                        "%s\n\n用户想学习：「%s」。\n" +
                        "请为用户生成一个学习计划，严格返回JSON格式（不要markdown代码块、不要任何注释、不要省略号）：\n" +
                        "{\"title\":\"计划标题\",\"description\":\"计划描述\",\"stages\":[{\"name\":\"阶段名\",\"description\":\"阶段描述\",\"tasks\":[\"任务1\",\"任务2\"]}]}\n" +
                        "要求：3-4个阶段，每个阶段2-3个任务，任务描述简短具体（每个任务不超过20字）。" +
                        "title和description不超过30字。必须返回完整闭合的JSON，不要截断。只返回JSON，不要其他文字。",
                        personalityPrompt, topic
                );
                String aiResponse = aiClient.chat("精灵帮用户生成学习计划", prompt);
                Map<String, Object> planData = parsePlanJson(aiResponse, topic);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> fallbackStages = (List<Map<String, Object>>) planData.get("stages");
                stageMaps = fallbackStages;
            }

            StudyPlan plan = new StudyPlan();
            plan.setUserId(userId);
            plan.setTitle("学习计划: " + topic);
            plan.setDescription("基于「" + topic + "」生成的学习计划");
            plan.setKnowledgePointIds(new ArrayList<>());
            plan.setKnowledgePointNames(new ArrayList<>());

            List<StudyPlan.PlanStage> stages = new ArrayList<>();
            if (stageMaps != null) {
                int order = 1;
                for (Map<String, Object> sm : stageMaps) {
                    StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
                    stage.setStageId(UUID.randomUUID().toString());
                    stage.setName((String) sm.getOrDefault("name", "阶段 " + order));
                    stage.setDescription((String) sm.getOrDefault("description", ""));
                    stage.setProgress(0);
                    stage.setStatus("NOT_STARTED");
                    stage.setOrder(order);
                    @SuppressWarnings("unchecked")
                    List<String> tasks = (List<String>) sm.get("tasks");
                    stage.setTasks(tasks);
                    stages.add(stage);
                    order++;
                }
            }
            // 兜底：如果一个阶段都没有，构造一个默认阶段
            if (stages.isEmpty()) {
                StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
                stage.setStageId(UUID.randomUUID().toString());
                stage.setName("入门：" + topic);
                stage.setDescription("从基础开始学习" + topic);
                stage.setProgress(0);
                stage.setStatus("NOT_STARTED");
                stage.setOrder(1);
                List<String> defaultTasks = new ArrayList<>();
                defaultTasks.add("了解" + topic + "的基本概念");
                defaultTasks.add("学习核心知识点");
                defaultTasks.add("动手实践小项目");
                stage.setTasks(defaultTasks);
                stages.add(stage);
            }
            plan.setStages(stages);

            StudyPlan created = studyPlanDubboService.create(plan);

            // 喂养精灵
            feed(userId, "plan_created", 5);

            return new QuickPlanResult(true, created.getId(), created.getTitle(),
                    "学习计划「" + created.getTitle() + "」已创建成功！包含" + stages.size() + "个阶段，加油吧！");
        } catch (Exception e) {
            log.error("快捷生成计划失败: userId={}, topic={}, error={}", userId, topic, e.getMessage());
            return new QuickPlanResult(false, null, null, "生成计划失败：" + e.getMessage());
        }
    }

    /** 解析AI服务返回的阶段JSON数组 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseStagesJson(String aiResponse) {
        ObjectMapper mapper = new ObjectMapper();
        if (aiResponse == null || aiResponse.trim().isEmpty()) return null;

        String json = aiResponse.trim();
        // 移除 markdown 代码块标记
        json = json.replaceAll("(?s)```json", "").replaceAll("(?s)```", "").trim();

        // 尝试1：直接解析为数组
        try {
            if (json.startsWith("[")) {
                return mapper.readValue(json, List.class);
            }
        } catch (Exception e) {
            log.warn("AI阶段JSON直接解析失败: {}", e.getMessage());
        }

        // 尝试2：截取 [ 到 ] 并修复
        try {
            int startIdx = json.indexOf("[");
            int endIdx = json.lastIndexOf("]");
            if (startIdx >= 0 && endIdx > startIdx) {
                String arrJson = json.substring(startIdx, endIdx + 1);
                String fixed = repairJson(arrJson);
                return mapper.readValue(fixed, List.class);
            }
        } catch (Exception e) {
            log.warn("AI阶段JSON修复解析失败: {}", e.getMessage());
        }

        // 尝试3：可能是对象格式 {stages:[...]}
        try {
            if (json.startsWith("{")) {
                Map<String, Object> map = parsePlanJson(aiResponse, "");
                Object stagesObj = map.get("stages");
                if (stagesObj instanceof List) {
                    return (List<Map<String, Object>>) stagesObj;
                }
            }
        } catch (Exception e) {
            log.warn("AI阶段JSON对象格式解析失败: {}", e.getMessage());
        }

        return null;
    }

    /** 容错解析AI返回的计划JSON */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePlanJson(String aiResponse, String topic) {
        ObjectMapper mapper = new ObjectMapper();
        if (aiResponse == null) return buildFallbackPlan(topic);

        String json = aiResponse.trim();
        // 移除 markdown 代码块标记
        json = json.replaceAll("(?s)```json", "").replaceAll("(?s)```", "").trim();
        // 截取第一个 { 到最后一个 }
        int startIdx = json.indexOf("{");
        int endIdx = json.lastIndexOf("}");
        if (startIdx >= 0 && endIdx > startIdx) {
            json = json.substring(startIdx, endIdx + 1);
        }

        // 尝试1：直接解析
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e1) {
            log.warn("AI计划JSON直接解析失败，尝试修复: {}", e1.getMessage());
        }

        // 尝试2：修复未闭合的 JSON（补齐括号、截断字符串等）
        try {
            String fixed = repairJson(json);
            return mapper.readValue(fixed, Map.class);
        } catch (Exception e2) {
            log.warn("AI计划JSON修复后仍解析失败: {}", e2.getMessage());
        }

        // 尝试3：截断到最后一个完整的 } 并重试
        try {
            String truncated = truncateToLastCompleteObject(json);
            if (truncated != null && !truncated.equals(json)) {
                String fixed = repairJson(truncated);
                return mapper.readValue(fixed, Map.class);
            }
        } catch (Exception e3) {
            log.warn("AI计划JSON截断修复失败: {}", e3.getMessage());
        }

        // 尝试4：使用兜底模板
        return buildFallbackPlan(topic);
    }

    /** 截断到最后一个完整的对象边界 */
    private String truncateToLastCompleteObject(String json) {
        if (json == null || json.isEmpty()) return null;
        // 从后往前找最后一个完整的 }, 然后看它是否在一个合理的位置
        // 找最后一个 }, 然后补齐外层括号
        int lastBrace = json.lastIndexOf('}');
        if (lastBrace < 0) return null;
        String truncated = json.substring(0, lastBrace + 1);
        return truncated;
    }

    /** 简单修复未闭合的 JSON：补齐缺失的 ] 和 } */
    private String repairJson(String json) {
        if (json == null || json.isEmpty()) return json;
        // 截掉末尾不完整的字符串 / 逗号
        StringBuilder sb = new StringBuilder(json);
        // 移除尾部多余逗号、省略号、非JSON字符
        while (sb.length() > 0) {
            char c = sb.charAt(sb.length() - 1);
            if (c == ',' || c == '.' || c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                sb.deleteCharAt(sb.length() - 1);
            } else {
                break;
            }
        }
        // 统计未闭合的 { [ "
        int braceCount = 0, bracketCount = 0;
        boolean inString = false, escape = false;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            else if (c == '[') bracketCount++;
            else if (c == ']') bracketCount--;
        }
        // 如果在字符串中，说明字符串被截断了，补一个引号
        if (inString) sb.append('"');
        // 补齐 ]
        for (int i = 0; i < bracketCount; i++) sb.append(']');
        // 补齐 }
        for (int i = 0; i < braceCount; i++) sb.append('}');
        return sb.toString();
    }

    /** 构造兜底计划数据 */
    private Map<String, Object> buildFallbackPlan(String topic) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("title", topic + " 学习计划");
        data.put("description", "AI 帮你定制的「" + topic + "」学习路径");
        List<Map<String, Object>> stages = new ArrayList<>();
        String[][] template = {
                {"入门基础", "了解核心概念", "查阅" + topic + "的入门资料", "搭建学习环境", "完成 Hello World 示例"},
                {"核心知识", "掌握关键技能", "学习" + topic + "的主要原理", "动手做小练习", "整理学习笔记"},
                {"实战应用", "做出一个项目", "构思一个小项目", "实现核心功能", "总结踩坑与心得"},
        };
        int order = 1;
        for (String[] s : template) {
            Map<String, Object> stage = new java.util.HashMap<>();
            stage.put("name", s[0]);
            stage.put("description", s[1]);
            List<String> tasks = new ArrayList<>();
            for (int i = 2; i < s.length; i++) tasks.add(s[i]);
            stage.put("tasks", tasks);
            stages.add(stage);
            order++;
        }
        data.put("stages", stages);
        return data;
    }

    @Override
    public SpiritState pomodoroTick(Long userId, int minutes) {
        SpiritState spirit = getSpiritState(userId);
        int decrease = Math.min(minutes, spirit.getSatiation());
        spirit.setSatiation(Math.max(0, spirit.getSatiation() - decrease));
        // 重新计算情绪
        SpiritEmotion newEmotion = emotionService.calculateEmotion(userId);
        spirit.setEmotion(newEmotion);
        spirit.setUpdateTime(java.time.LocalDateTime.now());
        spiritStateRepository.save(spirit);
        // 清除缓存
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        log.debug("番茄钟扣减饱食度: userId={}, minutes={}, decrease={}, satiation={}",
                userId, minutes, decrease, spirit.getSatiation());
        return getSpiritState(userId);
    }
}
