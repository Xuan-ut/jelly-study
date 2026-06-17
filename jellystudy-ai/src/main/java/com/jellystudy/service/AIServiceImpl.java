package com.jellystudy.service;

import com.jellystudy.dubbo.AIDubboService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.AIDubboService")
public class AIServiceImpl implements AIDubboService {

    @Autowired
    private LLMClient llmClient;

    @Override
    public String chat(String message) {
        String prompt = String.format(
            "你是JellyStudy学习平台的AI助手，专门帮助用户解答学习相关问题。请用自然、友好的语言回答以下问题：\n\n问题：%s",
            message
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "抱歉，我暂时无法回答您的问题。错误：" + e.getMessage();
        }
    }

    @Override
    public String analyzeComments(List<String> comments) {
        if (comments == null || comments.isEmpty()) {
            return "暂无评论数据可分析。";
        }
        
        String commentsText = String.join("\n\n", comments);
        String prompt = String.format(
            "请分析以下评论内容，完成以下任务：\n\n" +
            "评论列表：\n%s\n\n" +
            "任务要求：\n" +
            "1. 评估所有评论的整体质量（积极/中性/消极比例）\n" +
            "2. 总结评论中的核心观点和建议\n" +
            "3. 提炼最有价值的反馈内容\n" +
            "4. 用简洁明了的中文输出分析结果",
            commentsText
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "分析失败：" + e.getMessage();
        }
    }

    @Override
    public String analyzeQuestion(String questionContent) {
        String prompt = String.format(
            "请分析以下问题：\n\n问题：%s\n\n" +
            "分析要求：\n" +
            "1. 识别问题涉及的核心知识点\n" +
            "2. 评估问题的难度级别（简单/中等/困难）\n" +
            "3. 提供问题的解答思路或方向\n" +
            "4. 用简洁明了的中文输出分析结果",
            questionContent
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "分析失败：" + e.getMessage();
        }
    }

    @Override
    public String summarizeText(String text) {
        String prompt = String.format(
            "请对以下文本进行总结：\n\n%s\n\n" +
            "要求：\n" +
            "1. 提取核心要点\n" +
            "2. 保持原意不变\n" +
            "3. 用简洁明了的中文输出总结结果",
            text
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "总结失败：" + e.getMessage();
        }
    }

    @Override
    public String generateStudyPath(String subject, String currentLevel, String goal, List<String> knowledgePoints) {
        String kpStr = knowledgePoints != null ? String.join("、", knowledgePoints) : "未指定";
        String prompt = String.format(
            "你是一位专业的学习规划师。请为用户生成详细的学习路径规划。\n\n" +
            "学习科目：%s\n" +
            "当前水平：%s\n" +
            "学习目标：%s\n" +
            "涉及知识点：%s\n\n" +
            "请按以下格式输出：\n" +
            "1. 学习路径总览（概述从当前水平到目标的整体路径）\n" +
            "2. 分阶段学习计划（每个阶段包含：阶段名称、学习目标、核心知识点、建议学习时长、学习方法建议）\n" +
            "3. 关键里程碑（标记重要的学习节点）\n" +
            "4. 学习资源推荐（推荐适合的学习资源类型）\n" +
            "5. 风险提示（可能遇到的学习困难和应对策略）",
            subject, currentLevel, goal, kpStr
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "学习路径生成失败：" + e.getMessage();
        }
    }

    @Override
    public String analyzeWeakPoints(Long userId, List<Map<String, Object>> studyRecords) {
        if (studyRecords == null || studyRecords.isEmpty()) {
            return "暂无学习记录可供分析。";
        }
        String recordsStr = studyRecords.stream()
            .map(r -> String.format("阶段[%s]: 进度%d%%, 状态%s",
                r.get("stageName"), r.get("progress"), r.get("status")))
            .collect(Collectors.joining("\n"));
        String prompt = String.format(
            "你是一位专业的学习分析师。请根据用户的学习记录分析其薄弱环节。\n\n" +
            "用户学习记录：\n%s\n\n" +
            "请按以下格式输出分析结果：\n" +
            "1. 薄弱环节识别（找出进度较低或停滞的阶段）\n" +
            "2. 原因分析（可能的学习障碍）\n" +
            "3. 针对性建议（针对每个薄弱环节给出具体改进措施）\n" +
            "4. 优先级排序（建议先攻克哪些薄弱点）\n" +
            "5. 预计提升路径（从薄弱到掌握的学习路径）",
            recordsStr
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "薄弱点分析失败：" + e.getMessage();
        }
    }

    @Override
    public String generateDailyPlan(Long userId, String planId, List<Map<String, Object>> recentProgress) {
        String progressStr = recentProgress != null ? recentProgress.stream()
            .map(r -> String.format("阶段[%s]: 进度%d%%",
                r.get("stageName"), r.get("progress")))
            .collect(Collectors.joining("\n")) : "暂无进度数据";
        String prompt = String.format(
            "你是一位专业的学习规划师。请根据用户当前的学习进度，生成今日学习计划。\n\n" +
            "当前学习进度：\n%s\n\n" +
            "请按以下格式输出今日学习计划：\n" +
            "1. 今日学习目标（具体、可量化的目标）\n" +
            "2. 时间安排（建议的学习时间段和内容分配）\n" +
            "3. 重点学习内容（今日应优先学习的内容）\n" +
            "4. 练习建议（配合学习的练习题或实践任务）\n" +
            "5. 复习计划（需要回顾的已学内容）\n" +
            "6. 检验标准（如何判断今日学习是否达标）",
            progressStr
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "每日计划生成失败：" + e.getMessage();
        }
    }

    @Override
    public String analyzeUserBehavior(Long userId, List<Map<String, Object>> activities) {
        if (activities == null || activities.isEmpty()) {
            return "暂无行为数据可供分析。";
        }
        String actStr = activities.stream()
            .map(a -> String.format("[%s] %s - %s (%s)",
                a.get("type"), a.get("target"), a.get("content") != null ? a.get("content") : "", a.get("time")))
            .limit(30)
            .collect(Collectors.joining("\n"));
        String prompt = String.format(
            "你是一位专业的学习行为分析师。请根据用户在平台上的行为记录，分析其学习特征和模式。\n\n" +
            "用户行为记录：\n%s\n\n" +
            "请按以下格式输出分析结果：\n" +
            "1. 学习活跃度评估（活跃程度和规律性）\n" +
            "2. 学习偏好分析（偏好的学习内容和方式）\n" +
            "3. 互动参与度（提问、评论、点赞等社交行为分析）\n" +
            "4. 学习效果评估（基于行为推断的学习效果）\n" +
            "5. 改进建议（如何提升学习效率和参与度）\n" +
            "6. 个性化推荐（基于行为模式的个性化学习建议）",
            actStr
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "行为分析失败：" + e.getMessage();
        }
    }

    @Override
    public String generatePlanDetail(String subject, String goal, int stageCount, String difficulty) {
        String stageHint = stageCount > 0
            ? "请规划" + stageCount + "个学习阶段，"
            : "请根据学习内容的复杂度自行决定合适的阶段数量，";
        String prompt = String.format(
            "你是一位专业的学习规划师。请为以下学习需求生成详细的学习计划。\n\n" +
            "学习科目：%s\n" +
            "学习目标：%s\n" +
            "难度级别：%s\n\n" +
            "%s每个阶段应包含明确的名称、详细描述、核心知识点、具体任务和预计时长。\n\n" +
            "请严格按照以下JSON格式输出（不要包含其他文字）：\n" +
            "[\n" +
            "  {\n" +
            "    \"name\": \"阶段名称\",\n" +
            "    \"description\": \"阶段详细描述，包含学习目标和内容\",\n" +
            "    \"estimatedHours\": 预计学习小时数(数字),\n" +
            "    \"keyTopics\": [\"核心知识点1\", \"核心知识点2\"],\n" +
            "    \"learningMethods\": \"学习方法建议\",\n" +
            "    \"tasks\": [\"具体任务1\", \"具体任务2\", \"具体任务3\"],\n" +
            "    \"milestones\": [\"里程碑1\", \"里程碑2\"]\n" +
            "  }\n" +
            "]",
            subject, goal, difficulty, stageHint
        );
        try {
            return llmClient.callLLM(prompt);
        } catch (Exception e) {
            return "计划生成失败：" + e.getMessage();
        }
    }
}