package com.jellystudy.companion.controller;

import com.jellystudy.companion.config.SpiritConfigProperties;
import com.jellystudy.companion.dto.Result;
import com.jellystudy.companion.dubbo.consumer.StudyPlanServiceConsumer;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.service.spirit.SpiritService;
import com.jellystudy.companion.service.timeline.KnowledgeTreeService;
import com.jellystudy.companion.service.timeline.TimelineService;
import com.jellystudy.companion.util.CompanionConverter;
import com.jellystudy.entity.SpiritChatResponseDTO;
import com.jellystudy.entity.SpiritGreetingDTO;
import com.jellystudy.entity.SpiritStateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/companion/spirit")
@Slf4j
@RequiredArgsConstructor
public class SpiritController {

    private final SpiritService spiritService;
    private final StudyPlanServiceConsumer studyPlanConsumer;
    private final KnowledgeTreeService knowledgeTreeService;
    private final SpiritConfigProperties spiritConfig;
    private final TimelineService timelineService;

    @GetMapping("/greet")
    public Result<SpiritGreetingDTO> greet(@RequestParam Long userId) {
        SpiritService.SpiritGreetingResult result = spiritService.getGreeting(userId);
        SpiritGreetingDTO dto = CompanionConverter.toSpiritGreetingDTO(
                result.getEmotion(), result.getGreeting(), result.getSuggestion());
        return Result.success(dto);
    }

    @GetMapping("/state")
    public Result<SpiritStateDTO> getState(@RequestParam Long userId) {
        SpiritState state = spiritService.getSpiritState(userId);
        return Result.success(CompanionConverter.toSpiritStateDTO(state));
    }

    @PostMapping("/chat")
    public Result<SpiritChatResponseDTO> chat(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId()
                : UUID.randomUUID().toString().substring(0, 8);
        // 如果前端传了personalityKey，先切换性格再聊天
        if (request.getPersonalityKey() != null && !request.getPersonalityKey().isEmpty()) {
            SpiritConfigProperties.PersonalityProfile profile = spiritConfig.getPersonality(request.getPersonalityKey());
            if (profile != null && profile.getPrompt() != null) {
                spiritService.switchPersonality(request.getUserId(), request.getPersonalityKey(), profile.getPrompt());
            }
        }
        SpiritService.SpiritChatResult result = spiritService.chat(
                request.getUserId(), sessionId, request.getMessage());
        SpiritChatResponseDTO dto = new SpiritChatResponseDTO();
        dto.setSessionId(result.getSessionId());
        dto.setSpiritMessage(result.getSpiritMessage());
        dto.setEmotion(result.getEmotion());
        dto.setIntent(result.getIntent());
        dto.setPlanId(result.getPlanId());
        dto.setPlanTitle(result.getPlanTitle());
        return Result.success(dto);
    }

    @PostMapping("/feed")
    public Result<SpiritStateDTO> feed(@RequestParam Long userId,
                                        @RequestParam String eventType,
                                        @RequestParam(defaultValue = "0") int feedValue) {
        SpiritState state = spiritService.feed(userId, eventType, feedValue);
        return Result.success(CompanionConverter.toSpiritStateDTO(state));
    }

    @PostMapping("/rename")
    public Result<SpiritStateDTO> rename(@RequestParam Long userId,
                                          @RequestParam String newName) {
        SpiritState state = spiritService.renameSpirit(userId, newName);
        return Result.success(CompanionConverter.toSpiritStateDTO(state));
    }

    // ===== 性格系统 =====

    /** 获取所有可用性格列表 */
    @GetMapping("/personalities")
    public Result<List<Map<String, String>>> getPersonalities() {
        return Result.success(spiritConfig.getPersonalityList());
    }

    /** 切换精灵性格 */
    @PostMapping("/personality")
    public Result<SpiritStateDTO> switchPersonality(@RequestParam Long userId,
                                                     @RequestParam String personalityKey) {
        SpiritConfigProperties.PersonalityProfile profile = spiritConfig.getPersonality(personalityKey);
        SpiritState state = spiritService.switchPersonality(userId, personalityKey, profile.getPrompt());
        // 仅在名字还是默认名时才更新为性格默认名
        if (profile.getName() != null && isDefaultName(state.getName())) {
            state = spiritService.renameSpirit(userId, profile.getName());
        }
        return Result.success(CompanionConverter.toSpiritStateDTO(state));
    }

    /** 判断是否为默认精灵名字（未被用户自定义修改过） */
    private boolean isDefaultName(String name) {
        if (name == null) return true;
        java.util.List<String> defaultNames = java.util.List.of("小光", "小傲", "小博", "小蜂", "小酷", "小智", "精灵");
        return defaultNames.contains(name);
    }

    /** 切换精灵外形 */
    @PostMapping("/appearance")
    public Result<SpiritStateDTO> switchAppearance(@RequestParam Long userId,
                                                    @RequestParam int targetLevel) {
        SpiritState state = spiritService.switchAppearance(userId, targetLevel);
        return Result.success(CompanionConverter.toSpiritStateDTO(state));
    }

    // ===== 今日计划与任务 =====

    /** 获取今日学习计划与每日任务 */
    @GetMapping("/today-tasks")
    public Result<Map<String, Object>> getTodayTasks(@RequestParam Long userId) {
        SpiritService.TodayTasksResult result = spiritService.getTodayTasks(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("plans", result.getPlans());
        data.put("tasks", result.getTasks());
        data.put("completedCount", result.getCompletedCount());
        data.put("totalCount", result.getTotalCount());
        return Result.success(data);
    }

    /** 切换每日任务完成状态，返回AI鼓励 */
    @PostMapping("/toggle-task")
    public Result<Map<String, Object>> toggleTask(@RequestBody ToggleTaskRequest request) {
        SpiritService.TaskToggleResult result = spiritService.toggleTask(
                request.getUserId(), request.getTaskId(), request.getDate());
        Map<String, Object> data = new HashMap<>();
        data.put("completed", result.isCompleted());
        data.put("encouragement", result.getEncouragement());
        data.put("emotion", result.getEmotion());
        return Result.success(data);
    }

    /** 快捷生成学习计划（根据主题用AI生成） */
    @PostMapping("/quick-plan")
    public Result<Map<String, Object>> quickPlan(@RequestBody QuickPlanRequest request) {
        SpiritService.QuickPlanResult result = spiritService.quickGeneratePlan(
                request.getUserId(), request.getTopic());
        Map<String, Object> data = new HashMap<>();
        data.put("success", result.isSuccess());
        data.put("planId", result.getPlanId());
        data.put("planTitle", result.getPlanTitle());
        data.put("message", result.getMessage());
        return Result.success(data);
    }

    // 简单请求体
    @lombok.Data
    public static class ToggleTaskRequest {
        private Long userId;
        private String taskId;
        private String date;
    }

    @lombok.Data
    public static class QuickPlanRequest {
        private Long userId;
        private String topic;
    }

    /** 获取当前性格 */
    @GetMapping("/personality")
    public Result<Map<String, String>> getCurrentPersonality(@RequestParam Long userId) {
        SpiritState state = spiritService.getSpiritState(userId);
        String currentKey = state.getPersonalityKey() != null ? state.getPersonalityKey() : "warm";
        SpiritConfigProperties.PersonalityProfile profile = spiritConfig.getPersonality(currentKey);
        Map<String, String> result = new HashMap<>();
        result.put("key", currentKey);
        result.put("name", profile.getName());
        result.put("icon", profile.getIcon());
        result.put("desc", profile.getDesc());
        return Result.success(result);
    }

    // ===== 主动聊天 =====

    /** 精灵主动发起话题 */
    @GetMapping("/proactive-chat")
    public Result<SpiritChatResponseDTO> proactiveChat(@RequestParam Long userId,
                                                        @RequestParam(required = false) String personalityKey) {
        // 如果前端传了personalityKey，先切换
        if (personalityKey != null && !personalityKey.isEmpty()) {
            SpiritConfigProperties.PersonalityProfile profile = spiritConfig.getPersonality(personalityKey);
            if (profile != null && profile.getPrompt() != null) {
                spiritService.switchPersonality(userId, personalityKey, profile.getPrompt());
            }
        }
        SpiritService.SpiritChatResult result = spiritService.proactiveChat(userId);
        SpiritChatResponseDTO dto = new SpiritChatResponseDTO();
        dto.setSessionId(result.getSessionId());
        dto.setSpiritMessage(result.getSpiritMessage());
        dto.setEmotion(result.getEmotion());
        return Result.success(dto);
    }

    /**
     * 读取时空预测的学习提醒（EarlyWarning），生成督促复习的消息。
     * 返回字段：
     *  - hasWarning: 是否有需要提醒的预警
     *  - emotion: 建议精灵表情（有高危预警时为 ANGRY）
     *  - message: 督促复习的消息
     *  - warnings: 预警列表
     */
    @GetMapping("/review-reminder")
    public Result<Map<String, Object>> reviewReminder(@RequestParam Long userId) {
        Map<String, Object> data = new HashMap<>();
        try {
            List<TimelineService.EarlyWarning> warnings = timelineService.getEarlyWarnings(userId);
            boolean hasWarning = warnings != null && !warnings.isEmpty();
            data.put("hasWarning", hasWarning);

            if (hasWarning) {
                // 按严重程度排序，CRITICAL > WARNING > INFO
                TimelineService.EarlyWarning top = warnings.stream()
                        .min((a, b) -> severityRank(b.getSeverity()) - severityRank(a.getSeverity()))
                        .orElse(warnings.get(0));

                boolean critical = "CRITICAL".equalsIgnoreCase(top.getSeverity())
                        || "WARNING".equalsIgnoreCase(top.getSeverity());
                data.put("emotion", critical ? "ANGRY" : "CALM");

                // 构造督促消息
                StringBuilder msg = new StringBuilder();
                if (critical) {
                    msg.append("⚠️ 你有知识点快忘了！\n");
                } else {
                    msg.append("📖 温馨提醒：\n");
                }
                if (top.getKnowledgePoint() != null && !top.getKnowledgePoint().isEmpty()) {
                    msg.append("「").append(top.getKnowledgePoint()).append("」");
                }
                if (top.getDescription() != null) {
                    msg.append(top.getDescription());
                }
                if (top.getRecommendedAction() != null) {
                    msg.append("\n建议：").append(top.getRecommendedAction());
                }
                data.put("message", msg.toString());

                // 精简的预警列表（最多3条）
                List<Map<String, Object>> list = new java.util.ArrayList<>();
                for (int i = 0; i < Math.min(3, warnings.size()); i++) {
                    TimelineService.EarlyWarning w = warnings.get(i);
                    Map<String, Object> wm = new HashMap<>();
                    wm.put("severity", w.getSeverity());
                    wm.put("knowledgePoint", w.getKnowledgePoint());
                    wm.put("description", w.getDescription());
                    wm.put("recommendedAction", w.getRecommendedAction());
                    list.add(wm);
                }
                data.put("warnings", list);
            } else {
                data.put("emotion", "HAPPY");
                data.put("message", "目前没有需要复习的内容，保持节奏！");
                data.put("warnings", java.util.Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("获取复习提醒失败: userId={}, error={}", userId, e.getMessage());
            data.put("hasWarning", false);
            data.put("emotion", "CALM");
            data.put("message", "");
            data.put("warnings", java.util.Collections.emptyList());
        }
        return Result.success(data);
    }

    /** 严重程度排序：CRITICAL=3, WARNING=2, INFO=1, 其他=0 */
    private int severityRank(String severity) {
        if (severity == null) return 0;
        switch (severity.toUpperCase()) {
            case "CRITICAL": return 3;
            case "WARNING": return 2;
            case "INFO": return 1;
            default: return 0;
        }
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(@RequestParam Long userId) {
        SpiritState state = spiritService.getSpiritState(userId);
        int streakDays = studyPlanConsumer.getStreakDays(userId);
        boolean studiedToday = studyPlanConsumer.hasStudiedToday(userId);

        KnowledgeTreeSnapshot tree = knowledgeTreeService.getLatestSnapshot(userId);
        long grayNodes = 0;
        if (tree != null && tree.getBranches() != null) {
            grayNodes = tree.getBranches().stream()
                    .flatMap(b -> b.getNodes() != null ? b.getNodes().stream() : java.util.stream.Stream.empty())
                    .flatMap(n -> n.getChildren() != null ? n.getChildren().stream() : java.util.stream.Stream.empty())
                    .filter(k -> k.getMastery() < 0.3)
                    .count();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("spirit", CompanionConverter.toSpiritStateDTO(state));
        result.put("streakDays", streakDays);
        result.put("studiedToday", studiedToday);
        result.put("grayNodesCount", grayNodes);
        return Result.success(result);
    }

    // 简单请求体
    @lombok.Data
    public static class ChatRequest {
        private Long userId;
        private String sessionId;
        private String message;
        private String personalityKey;
    }

    // ===== 番茄钟 =====

    /** 番茄钟计时扣减饱食度 */
    @PostMapping("/pomodoro-tick")
    public Result<SpiritStateDTO> pomodoroTick(@RequestParam Long userId,
                                                @RequestParam(defaultValue = "1") int minutes) {
        SpiritState state = spiritService.pomodoroTick(userId, minutes);
        return Result.success(CompanionConverter.toSpiritStateDTO(state));
    }
}
