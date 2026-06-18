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
    private final org.springframework.context.ApplicationContext applicationContext;

    /** 检测用户消息是否包含「生成学习计划」的意图 */
    private boolean detectPlanIntent(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        // 触发关键词：生成/创建/制定/帮我做 + 学习/计划/规划
        boolean hasAction = lower.contains("生成") || lower.contains("创建") || lower.contains("制定")
                || lower.contains("帮我做") || lower.contains("帮我搞") || lower.contains("帮我建")
                || lower.contains("帮我安排") || lower.contains("帮我规划") || lower.contains("做一个")
                || lower.contains("做个") || lower.contains("来一个") || lower.contains("给我一个")
                || lower.contains("制作");
        boolean hasObject = lower.contains("学习计划") || lower.contains("学习规划")
                || lower.contains("学习方案") || lower.contains("学习路线");
        return hasAction && hasObject;
    }

    /** 从消息中提取学习主题 */
    private String extractTopic(String message) {
        if (message == null) return null;
        // 去掉常见动词词尾
        String topic = message;
        String[] removes = {"帮我生成", "帮我创建", "帮我制定", "帮我安排", "帮我规划",
                "帮我做一个", "帮我做个", "帮我建一个", "帮我搞一个", "给我一个", "给我做一个",
                "生成一个", "创建一个", "制定一个", "做一个", "做个", "来一个",
                "学习计划", "学习规划", "学习方案", "学习路线",
                "的", "关于", "想要", "想学", "我想", "请", "可以吗", "可以", "吗", "啊", "呢", "哦", "呀"};
        for (String r : removes) {
            topic = topic.replace(r, " ");
        }
        topic = topic.replaceAll("\\s+", " ").trim();
        return topic.isEmpty() ? null : topic;
    }

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

        // ===== 意图识别：检测「生成学习计划」 =====
        if (detectPlanIntent(message)) {
            String topic = extractTopic(message);
            if (topic != null && topic.length() > 0 && topic.length() < 80) {
                try {
                    SpiritService spiritService = applicationContext.getBean(SpiritService.class);
                    SpiritService.QuickPlanResult planResult = spiritService.quickGeneratePlan(spirit.getUserId(), topic);
                    if (planResult.isSuccess()) {
                        String reply = "好咧～我已经为你生成了学习计划「" + planResult.getPlanTitle() + "」，快去查看吧！🎉";
                        SpiritChatResult result = new SpiritChatResult(sessionId, reply, spirit.getEmotion().name());
                        result.setIntent("generated_plan");
                        result.setPlanId(planResult.getPlanId());
                        result.setPlanTitle(planResult.getPlanTitle());
                        // 记录交互
                        spirit.getMemory().setLastInteraction(LocalDateTime.now());
                        spirit.setUpdateTime(LocalDateTime.now());
                        spiritStateRepository.save(spirit);
                        redisTemplate.delete(RedisKeyConstants.spiritState(spirit.getUserId()));
                        return result;
                    } else {
                        String reply = "唔...生成计划失败了：" + planResult.getMessage() + "，要不你换个主题再说一次？";
                        SpiritChatResult result = new SpiritChatResult(sessionId, reply, spirit.getEmotion().name());
                        result.setIntent("normal");
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("聊天中意图识别生成计划失败: {}", e.getMessage());
                    // fallback to normal chat
                }
            }
        }
        // ===== 普通聊天 =====

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

        // 随机选择聊天类型，让内容更丰富
        String[] chatTypes = {
                "学习鼓励或提醒",
                "分享一个有趣的小知识或冷知识",
                "讲一个简短的冷笑话或脑筋急转弯",
                "聊聊天气、心情或生活小感悟",
                "推荐一个学习方法或小技巧",
                "出一个趣味小问题考考用户"
        };
        String chatType = chatTypes[(int) (Math.random() * chatTypes.length)];

        String prompt = String.format(
                "%s\n\n" +
                "当前精灵状态：%s(Lv.%d)，情感：%s\n" +
                "用户最近学习：%s，连续学习%d天，今天%s学习\n\n" +
                "请你主动发起一个话题和用户聊天。本次聊天类型：%s\n\n" +
                "要求：\n" +
                "1. 根据你的性格特点说话，展现个性\n" +
                "2. 如果是学习鼓励，要真诚温暖；如果是冷笑话，要出其不意\n" +
                "3. 如果用户今天还没学习，可以温柔但有趣地提醒\n" +
                "4. 语气自然活泼，像朋友一样，不要像机器人\n" +
                "5. 控制在30字以内，简洁有力\n" +
                "6. 不要用「让我想想」「嗯...」等拖延词\n" +
                "7. 可以适当用emoji增加趣味",
                personalityPrompt,
                spirit.getLevelName(), spirit.getLevel(),
                spirit.getEmotion().getDisplayName(),
                recentTopic, streakDays,
                studiedToday ? "已经" : "还没",
                chatType
        );

        String aiResponse = aiClient.chat(
                spirit.getEmotion().getDisplayName() + "的精灵主动聊天",
                prompt
        );

        // 检查AI是否返回了降级响应，如果是则使用本地丰富话题
        if (aiResponse == null || aiResponse.contains("走神") || aiResponse.contains("再说一遍")) {
            aiResponse = getLocalProactiveMessage(spirit, streakDays, studiedToday, recentTopic);
        }

        return new SpiritChatResult(sessionId, aiResponse, spirit.getEmotion().name());
    }

    /** 本地丰富话题降级策略 */
    private String getLocalProactiveMessage(SpiritState spirit, int streakDays,
                                              boolean studiedToday, String recentTopic) {
        List<String> messages = new ArrayList<>();

        // 根据学习状态
        if (!studiedToday) {
            messages.add("今天还没翻开书本呢，要不要来一题热热身？📖");
            messages.add("嘿，学习进度条还空着哦，来充个电吧！⚡");
            messages.add("偷偷告诉你，现在开始学的话效率最高~🤫");
        }
        if (streakDays >= 7) {
            messages.add("连续" + streakDays + "天！你就是学习界的永动机！🔥");
            messages.add("哇，" + streakDays + "天打卡！给你颁个坚持奖🏆");
        }
        if (streakDays > 0 && streakDays < 3) {
            messages.add("才" + streakDays + "天，坚持下去就能解锁新成就！");
            messages.add("连续学习的感觉是不是很棒？继续冲！");
        }

        // 趣味话题
        messages.add("你知道吗？章鱼有三颗心脏！🐙 学习也得多用心哦~");
        messages.add("脑筋急转弯：什么门永远关不上？答案：球门！⚽");
        messages.add("今天想挑战什么？我给你当啦啦队！📣");
        messages.add("听说边学边喝水效率更高，你喝水了吗？💧");
        messages.add("休息也是学习的一部分哦，但别休息太久~😏");
        messages.add("如果知识是食物，你今天吃饱了吗？🍔");
        messages.add("小知识：番茄工作法，学25分钟休5分钟，超好用！🍅");
        messages.add("你有没有试过把知识点讲给别人听？那才是真学会了！");

        // 根据性格微调
        String emotion = spirit.getEmotion().name();
        if ("EXCITED".equals(emotion)) {
            messages.add("啊啊啊好想学习！一起来吧！！🌟");
            messages.add("冲冲冲！今天也要元气满满！💪");
        } else if ("SLEEPY".equals(emotion)) {
            messages.add("好困...但学习使我清醒...大概吧😴");
            messages.add("zzZ...啊不是在睡，是在冥想学习！💭");
        }

        return messages.get((int) (Math.random() * messages.size()));
    }
}
