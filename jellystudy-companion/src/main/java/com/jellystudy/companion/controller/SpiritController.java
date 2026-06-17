package com.jellystudy.companion.controller;

import com.jellystudy.companion.config.SpiritConfigProperties;
import com.jellystudy.companion.dto.Result;
import com.jellystudy.companion.dubbo.consumer.StudyPlanServiceConsumer;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.service.spirit.SpiritService;
import com.jellystudy.companion.service.timeline.KnowledgeTreeService;
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
        var defaultNames = java.util.List.of("小光", "小傲", "小博", "小蜂", "小酷", "小智", "精灵");
        return defaultNames.contains(name);
    }

    /** 切换精灵外形 */
    @PostMapping("/appearance")
    public Result<SpiritStateDTO> switchAppearance(@RequestParam Long userId,
                                                    @RequestParam int targetLevel) {
        SpiritState state = spiritService.switchAppearance(userId, targetLevel);
        return Result.success(CompanionConverter.toSpiritStateDTO(state));
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
}
