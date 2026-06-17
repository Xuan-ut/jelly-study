package com.jellystudy.companion.service.spirit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.companion.ai.AIClient;
import com.jellystudy.companion.ai.prompt.SpiritPromptBuilder;
import com.jellystudy.companion.ai.prompt.SpiritPromptBuilder.ChatMessage;
import com.jellystudy.companion.ai.prompt.SpiritPromptBuilder.UserContext;
import com.jellystudy.companion.constant.RedisKeyConstants;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.repository.SpiritStateRepository;
import com.jellystudy.companion.service.spirit.SpiritService.SpiritChatResult;
import com.jellystudy.companion.service.spirit.SpiritService.SpiritGreetingResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 精灵对话系统
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpiritChatServiceImpl implements SpiritChatService {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final AIClient aiClient;
    private final SpiritPromptBuilder promptBuilder;
    private final StringRedisTemplate redisTemplate;
    private final SpiritStateRepository spiritStateRepository;
    private final com.jellystudy.companion.dubbo.consumer.StudyPlanServiceConsumer studyPlanConsumer;

    @Override
    public SpiritGreetingResult getGreeting(SpiritState spirit) {
        UserContext ctx = new UserContext();
        ctx.setCurrentTopic(spirit.getMemory().getRecentTopics().isEmpty()
                ? "暂无" : spirit.getMemory().getRecentTopics().get(0));
        ctx.setLastStudyTopic(ctx.getCurrentTopic());
        // 使用真实学习数据
        ctx.setStreakDays(studyPlanConsumer.getStreakDays(spirit.getUserId()));
        ctx.setStudiedToday(studyPlanConsumer.hasStudiedToday(spirit.getUserId()));

        String prompt = promptBuilder.buildGreetingPrompt(spirit, ctx);
        String aiResponse = aiClient.chat(
                spirit.getEmotion().getDisplayName() + "的精灵向用户问候",
                prompt
        );

        String suggestion = spirit.getLevel() >= 1
                ? "今天要不要学点什么新东西？" : "";

        return new SpiritGreetingResult(
                spirit.getEmotion().name(),
                aiResponse,
                suggestion
        );
    }

    @Override
    @SneakyThrows
    public SpiritChatResult chat(SpiritState spirit, String sessionId, String message) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().substring(0, 8);
        }

        String historyKey = RedisKeyConstants.chatHistory(spirit.getUserId(), sessionId);
        String historyJson = redisTemplate.opsForValue().get(historyKey);
        List<ChatMessage> history = new ArrayList<>();
        if (historyJson != null) {
            history = objectMapper.readValue(historyJson,
                    new TypeReference<List<ChatMessage>>() {});
        }

        String prompt = promptBuilder.buildChatPrompt(spirit, history, message);
        String aiResponse = aiClient.chat(
                spirit.getEmotion().getDisplayName() + "的精灵与用户对话",
                prompt
        );

        ChatMessage userMsg = new ChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(message);
        history.add(userMsg);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setRole("spirit");
        aiMsg.setContent(aiResponse);
        history.add(aiMsg);

        if (history.size() > 20) {
            history = history.subList(history.size() - 20, history.size());
        }
        redisTemplate.opsForValue().set(historyKey,
                objectMapper.writeValueAsString(history), 30, TimeUnit.MINUTES);

        spirit.getMemory().setLastInteraction(LocalDateTime.now());
        spirit.getMemory().getRecentTopics().add(0,
                message.length() > 20 ? message.substring(0, 20) : message);
        if (spirit.getMemory().getRecentTopics().size() > 10) {
            spirit.getMemory().getRecentTopics()
                    .subList(10, spirit.getMemory().getRecentTopics().size()).clear();
        }

        // 保存记忆到数据库并刷新缓存
        spirit.setUpdateTime(LocalDateTime.now());
        spiritStateRepository.save(spirit);
        String stateKey = RedisKeyConstants.spiritState(spirit.getUserId());
        redisTemplate.delete(stateKey);

        return new SpiritChatResult(sessionId, aiResponse, spirit.getEmotion().name());
    }

    @Override
    @SneakyThrows
    public SpiritChatResult proactiveChat(SpiritState spirit) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        // 构建主动聊天的prompt
        String personalityPrompt = spirit.getPersonalityPrompt() != null
                ? spirit.getPersonalityPrompt()
                : "你是一个名叫小光的AI学习精灵，性格好奇、温暖、偶尔调皮。";

        String recentTopic = spirit.getMemory().getRecentTopics().isEmpty()
                ? "暂无" : spirit.getMemory().getRecentTopics().get(0);
        int streakDays = studyPlanConsumer.getStreakDays(spirit.getUserId());
        boolean studiedToday = studyPlanConsumer.hasStudiedToday(spirit.getUserId());

        String prompt = String.format(
                "%s\n\n" +
                "当前精灵状态：%s(Lv.%d)，情感：%s\n" +
                "用户最近学习：%s，连续学习%d天，今天%s学习\n\n" +
                "请你主动发起一个话题和用户聊天。要求：\n" +
                "1. 根据你的性格特点说话\n" +
                "2. 可以是学习鼓励、生活闲聊、分享小知识、讲个冷笑话等\n" +
                "3. 如果用户今天还没学习，可以温柔地提醒\n" +
                "4. 语气自然，像朋友一样，不要像机器人\n" +
                "5. 控制在30字以内",
                personalityPrompt,
                spirit.getLevelName(), spirit.getLevel(),
                spirit.getEmotion().getDisplayName(),
                recentTopic, streakDays,
                studiedToday ? "已经" : "还没"
        );

        String aiResponse = aiClient.chat(
                spirit.getEmotion().getDisplayName() + "的精灵主动聊天",
                prompt
        );

        return new SpiritChatResult(sessionId, aiResponse, spirit.getEmotion().name());
    }
}
