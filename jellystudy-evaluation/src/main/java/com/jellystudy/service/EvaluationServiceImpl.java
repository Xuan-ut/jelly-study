package com.jellystudy.service;

import com.jellystudy.dubbo.AIDubboService;
import com.jellystudy.dubbo.EvaluationDubboService;
import lombok.extern.slf4j.Slf4j;
import com.jellystudy.entity.AnswerEvaluation;
import com.jellystudy.entity.QuestionEvaluation;
import com.jellystudy.repository.AnswerEvaluationRepository;
import com.jellystudy.repository.QuestionEvaluationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.EvaluationDubboService")
public class EvaluationServiceImpl implements EvaluationDubboService {

    @Autowired
    private QuestionEvaluationRepository questionEvaluationRepository;

    @Autowired
    private AnswerEvaluationRepository answerEvaluationRepository;

    @Autowired
    private LLMClient llmClient;

    @Autowired
    private ObjectMapper objectMapper;
    
    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.AIDubboService", check = false)
    private AIDubboService aiService;

    @Override
    public QuestionEvaluation evaluateQuestion(String questionContent) {
        try {
            String knowledgePointsResult = extractKnowledgePointsByLLM(questionContent);
            List<String> knowledgePoints = parseKnowledgePoints(knowledgePointsResult);
            
            String difficultyResult = evaluateDifficultyByLLM(questionContent);
            String difficulty = parseDifficulty(difficultyResult);
            
            QuestionEvaluation evaluation = new QuestionEvaluation();
            evaluation.setId(UUID.randomUUID().toString());
            evaluation.setKnowledgePoints(knowledgePoints);
            evaluation.setDifficulty(difficulty);
            evaluation.setEvaluateTime(new Date());
            
            log.info("LLM评估问题完成: 难度={}, 知识点={}", difficulty, knowledgePoints);
            return questionEvaluationRepository.save(evaluation);
        } catch (Exception e) {
            log.warn("LLM评估问题失败，降级到本地分析: {}", e.getMessage());
            List<String> knowledgePoints = extractKnowledgePoints(questionContent);
            String difficulty = evaluateDifficulty(questionContent);
            
            QuestionEvaluation evaluation = new QuestionEvaluation();
            evaluation.setId(UUID.randomUUID().toString());
            evaluation.setKnowledgePoints(knowledgePoints);
            evaluation.setDifficulty(difficulty);
            evaluation.setEvaluateTime(new Date());
            
            return questionEvaluationRepository.save(evaluation);
        }
    }

    @Override
    public AnswerEvaluation evaluateAnswer(String questionContent, String answerContent, String scoreType) {
        try {
            String result = evaluateAnswerByLLM(questionContent, answerContent, scoreType);
            JsonNode root = objectMapper.readTree(result);
            int score = root.has("score") ? root.get("score").asInt() : 0;
            String feedback = root.has("feedback") ? root.get("feedback").asText() : "";
            
            AnswerEvaluation evaluation = new AnswerEvaluation();
            evaluation.setId(UUID.randomUUID().toString());
            evaluation.setScore(score);
            evaluation.setScoreType(scoreType);
            evaluation.setFeedback(feedback);
            evaluation.setEvaluateTime(new Date());
            
            log.info("LLM评估回答完成: 分数={}, 反馈={}", score, feedback);
            return answerEvaluationRepository.save(evaluation);
        } catch (Exception e) {
            log.warn("LLM评估回答失败，降级到本地分析: {}", e.getMessage());
            int score = calculateScore(questionContent, answerContent, scoreType);
            String feedback = generateFeedback(score, scoreType);
            
            AnswerEvaluation evaluation = new AnswerEvaluation();
            evaluation.setId(UUID.randomUUID().toString());
            evaluation.setScore(score);
            evaluation.setScoreType(scoreType);
            evaluation.setFeedback(feedback);
            evaluation.setEvaluateTime(new Date());
            
            return answerEvaluationRepository.save(evaluation);
        }
    }

    @Override
    public Map<String, Object> evaluateComment(String questionContent, String commentContent) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            String llmResult = evaluateCommentByLLM(questionContent, commentContent);
            JsonNode root = objectMapper.readTree(llmResult);
            int score = root.has("score") ? root.get("score").asInt() : 50;
            String feedback = root.has("feedback") ? root.get("feedback").asText() : "";
            result.put("score", score);
            result.put("feedback", feedback);
            log.info("LLM评估评论完成: 分数={}, 反馈={}", score, feedback);
        } catch (Exception e) {
            log.warn("LLM评估评论失败，降级到本地分析: {}", e.getMessage());
            int score = 50;
            if (commentContent.length() > 50) score += 10;
            if (commentContent.contains("因为") || commentContent.contains("所以")) score += 5;
            if (commentContent.contains("同意") || commentContent.contains("正确")) score -= 5;
            result.put("score", Math.min(100, Math.max(0, score)));
            result.put("feedback", "本地降级评估");
        }
        return result;
    }

    public QuestionEvaluation evaluateQuestionWithLLM(String questionContent) {
        String knowledgePoints = extractKnowledgePointsByLLM(questionContent);
        String difficulty = evaluateDifficultyByLLM(questionContent);
        
        List<String> pointList = parseKnowledgePoints(knowledgePoints);
        
        QuestionEvaluation evaluation = new QuestionEvaluation();
        evaluation.setId(UUID.randomUUID().toString());
        evaluation.setKnowledgePoints(pointList);
        evaluation.setDifficulty(difficulty);
        evaluation.setEvaluateTime(new Date());
        
        return questionEvaluationRepository.save(evaluation);
    }

    public AnswerEvaluation evaluateAnswerWithLLM(String questionContent, String answerContent, String scoreType) {
        String result = evaluateAnswerByLLM(questionContent, answerContent, scoreType);
        
        try {
            JsonNode root = objectMapper.readTree(result);
            int score = root.has("score") ? root.get("score").asInt() : calculateScore(questionContent, answerContent, scoreType);
            String feedback = root.has("feedback") ? root.get("feedback").asText() : generateFeedback(score, scoreType);
            
            AnswerEvaluation evaluation = new AnswerEvaluation();
            evaluation.setId(UUID.randomUUID().toString());
            evaluation.setScore(score);
            evaluation.setScoreType(scoreType);
            evaluation.setFeedback(feedback);
            evaluation.setEvaluateTime(new Date());
            
            return answerEvaluationRepository.save(evaluation);
        } catch (Exception e) {
            return evaluateAnswer(questionContent, answerContent, scoreType);
        }
    }

    private String extractKnowledgePointsByLLM(String questionContent) {
        String prompt = String.format(
            "你是一个专业的编程教育评估专家。请从以下问题中提取涉及的知识点。\n\n" +
            "问题：%s\n\n" +
            "要求：只返回知识点名称的JSON数组，如[\"知识点1\", \"知识点2\"]，不要返回其他内容。",
            questionContent
        );
        return llmClient.callLLM(prompt);
    }

    private String evaluateDifficultyByLLM(String questionContent) {
        String prompt = String.format(
            "你是一个专业的编程教育评估专家。请评估以下问题的难度等级。\n\n" +
            "问题：%s\n\n" +
            "评分标准（百分制）：\n" +
            "- 简单(1-40分)：基础概念、语法使用、简单操作\n" +
            "- 中等(41-70分)：框架应用、原理理解、综合运用\n" +
            "- 困难(71-100分)：架构设计、底层原理、性能优化、分布式\n\n" +
            "要求：以JSON格式返回，格式为{\"difficulty\": \"简单/中等/困难\", \"score\": 数字, \"reason\": \"评估理由\"}，不要返回其他内容。",
            questionContent
        );
        return llmClient.callLLM(prompt);
    }

    private String evaluateAnswerByLLM(String questionContent, String answerContent, String scoreType) {
        String prompt = String.format(
            "你是一个专业的编程教育评估专家。请对以下回答进行百分制评分。\n\n" +
            "问题：%s\n\n" +
            "回答：%s\n\n" +
            "评分标准（百分制）：\n" +
            "- 90-100分：回答非常优秀，逻辑清晰，内容完整，有深度分析\n" +
            "- 75-89分：回答良好，基本覆盖要点，有一定深度\n" +
            "- 60-74分：回答及格，覆盖部分要点，但不够深入\n" +
            "- 40-59分：回答较差，要点缺失较多\n" +
            "- 0-39分：回答不合格，与问题无关或严重错误\n\n" +
            "要求：以JSON格式返回，格式为{\"score\": 分数(0-100的整数), \"feedback\": \"评价反馈\", \"scoreType\": \"100\"}，不要返回其他内容。",
            questionContent, answerContent
        );
        return llmClient.callLLM(prompt);
    }

    private String evaluateCommentByLLM(String questionContent, String commentContent) {
        String prompt = String.format(
            "你是一个专业的编程教育评估专家。请对以下评论进行百分制评分。\n\n" +
            "问题：%s\n\n" +
            "评论：%s\n\n" +
            "评分标准（百分制）：\n" +
            "- 90-100分：评论非常有价值，提供了深入见解或补充了重要信息\n" +
            "- 75-89分：评论有价值，有建设性意见或合理分析\n" +
            "- 60-74分：评论一般，有一定参考价值\n" +
            "- 40-59分：评论价值较低，缺乏实质内容\n" +
            "- 0-39分：评论无价值，与问题无关或包含不当内容\n\n" +
            "要求：以JSON格式返回，格式为{\"score\": 分数(0-100的整数), \"feedback\": \"评价反馈\"}，不要返回其他内容。",
            questionContent, commentContent
        );
        return llmClient.callLLM(prompt);
    }
    
    private String parseDifficulty(String llmResult) {
        try {
            JsonNode root = objectMapper.readTree(llmResult);
            String difficulty = root.has("difficulty") ? root.get("difficulty").asText() : "中等";
            if (difficulty.contains("简") || difficulty.contains("易") || difficulty.equalsIgnoreCase("easy")) {
                return "简单";
            } else if (difficulty.contains("困") || difficulty.contains("难") || difficulty.equalsIgnoreCase("hard")) {
                return "困难";
            } else {
                return "中等";
            }
        } catch (Exception e) {
            log.warn("解析LLM难度结果失败: {}", e.getMessage());
            return "中等";
        }
    }

    private List<String> parseKnowledgePoints(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return extractKnowledgePoints(jsonString);
        }
    }

    @Override
    public QuestionEvaluation saveQuestionEvaluation(QuestionEvaluation evaluation) {
        if (evaluation.getId() == null) {
            evaluation.setId(UUID.randomUUID().toString());
        }
        if (evaluation.getEvaluateTime() == null) {
            evaluation.setEvaluateTime(new Date());
        }
        return questionEvaluationRepository.save(evaluation);
    }

    @Override
    public QuestionEvaluation findByQuestionId(String questionId) {
        return questionEvaluationRepository.findByQuestionId(questionId);
    }

    @Override
    public AnswerEvaluation saveAnswerEvaluation(AnswerEvaluation evaluation) {
        if (evaluation.getId() == null) {
            evaluation.setId(UUID.randomUUID().toString());
        }
        if (evaluation.getEvaluateTime() == null) {
            evaluation.setEvaluateTime(new Date());
        }
        return answerEvaluationRepository.save(evaluation);
    }

    @Override
    public QuestionEvaluation getQuestionEvaluation(String questionId) {
        return questionEvaluationRepository.findByQuestionId(questionId);
    }

    @Override
    public AnswerEvaluation getAnswerEvaluation(String answerId) {
        return answerEvaluationRepository.findByAnswerId(answerId);
    }

    @Override
    public List<QuestionEvaluation> getQuestionEvaluationsByQuestionIds(List<String> questionIds) {
        return questionEvaluationRepository.findByQuestionIdIn(questionIds);
    }

    @Override
    public List<AnswerEvaluation> getAnswerEvaluationsByAnswerIds(List<String> answerIds) {
        return answerEvaluationRepository.findByAnswerIdIn(answerIds);
    }

    @Override
    public long countQuestionEvaluations() {
        return questionEvaluationRepository.count();
    }

    @Override
    public long countAnswerEvaluations() {
        return answerEvaluationRepository.count();
    }

    @Override
    public String chat(String message) {
        String prompt = String.format(
            "你是JellyStudy学习平台的AI助手，专门帮助用户解答学习相关问题。请用自然、友好的语言回答以下问题：\n\n问题：%s",
            message
        );
        return llmClient.callLLM(prompt);
    }

    @Override
    public String analyzeComments(List<String> comments) {
        if (comments == null || comments.isEmpty()) {
            return "暂无评论数据可分析。";
        }
        
        // 调用 AI 服务进行评论分析
        try {
            log.info("调用 AI 服务分析评论，共 {} 条", comments.size());
            return aiService.analyzeComments(comments);
        } catch (Exception e) {
            log.warn("调用 AI 服务失败，降级到本地分析: {}", e.getMessage());
            // 降级到本地分析
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
            return llmClient.callLLM(prompt);
        }
    }
    
    /**
     * 智能分析问题（调用 AI 服务）
     */
    public String intelligentAnalyzeQuestion(String questionContent) {
        try {
            log.info("调用 AI 服务智能分析问题");
            return aiService.analyzeQuestion(questionContent);
        } catch (Exception e) {
            log.warn("调用 AI 服务失败: {}", e.getMessage());
            return "分析失败：" + e.getMessage();
        }
    }
    
    /**
     * 智能总结回答（调用 AI 服务）
     */
    public String intelligentSummarizeAnswer(String answerContent) {
        try {
            log.info("调用 AI 服务总结回答");
            return aiService.summarizeText(answerContent);
        } catch (Exception e) {
            log.warn("调用 AI 服务失败: {}", e.getMessage());
            return "总结失败：" + e.getMessage();
        }
    }

    private List<String> extractKnowledgePoints(String questionContent) {
        List<String> points = new ArrayList<>();
        
        if (questionContent.contains("Java") || questionContent.contains("java")) {
            points.add("Java基础");
        }
        if (questionContent.contains("Spring") || questionContent.contains("spring")) {
            points.add("Spring框架");
        }
        if (questionContent.contains("数据库") || questionContent.contains("MySQL") || questionContent.contains("MongoDB")) {
            points.add("数据库技术");
        }
        if (questionContent.contains("算法") || questionContent.contains("数据结构")) {
            points.add("算法与数据结构");
        }
        if (questionContent.contains("分布式") || questionContent.contains("Dubbo") || questionContent.contains("微服务")) {
            points.add("分布式系统");
        }
        if (questionContent.contains("设计模式")) {
            points.add("设计模式");
        }
        if (questionContent.contains("网络") || questionContent.contains("HTTP")) {
            points.add("计算机网络");
        }
        if (questionContent.contains("并发") || questionContent.contains("多线程")) {
            points.add("并发编程");
        }
        
        if (points.isEmpty()) {
            points.add("综合知识");
        }
        
        return points;
    }

    private String evaluateDifficulty(String questionContent) {
        int complexityScore = 0;
        
        List<String> hardKeywords = Arrays.asList("架构", "分布式", "微服务", "高并发", "性能优化", "源码", "底层原理", "JVM", "调优", "集群");
        List<String> mediumKeywords = Arrays.asList("Spring", "框架", "事务", "缓存", "消息队列", "设计模式", "索引", "中间件", "负载均衡", "容器");
        List<String> easyKeywords = Arrays.asList("基础", "入门", "语法", "变量", "循环", "数组", "类", "对象", "方法", "安装");
        
        for (String keyword : hardKeywords) {
            if (questionContent.contains(keyword)) complexityScore += 3;
        }
        for (String keyword : mediumKeywords) {
            if (questionContent.contains(keyword)) complexityScore += 2;
        }
        for (String keyword : easyKeywords) {
            if (questionContent.contains(keyword)) complexityScore -= 1;
        }
        
        if (questionContent.length() > 300) complexityScore += 2;
        else if (questionContent.length() > 150) complexityScore += 1;
        
        if (questionContent.contains("为什么") || questionContent.contains("原理") || questionContent.contains("如何实现")) {
            complexityScore += 2;
        }
        if (questionContent.contains("是什么") || questionContent.contains("怎么用") || questionContent.contains("如何使用")) {
            complexityScore -= 1;
        }
        
        if (complexityScore >= 5) return "困难";
        else if (complexityScore >= 2) return "中等";
        else return "简单";
    }

    private int calculateScore(String questionContent, String answerContent, String scoreType) {
        int baseScore = 60;
        
        double lengthRatio = (double) answerContent.length() / Math.max(questionContent.length(), 1);
        if (lengthRatio >= 2.0) baseScore += 12;
        else if (lengthRatio >= 1.0) baseScore += 8;
        else if (lengthRatio >= 0.5) baseScore += 3;
        else baseScore -= 5;
        
        List<String> depthKeywords = Arrays.asList("原理", "实现", "机制", "优化", "设计", "架构", "底层", "源码", "分析", "对比");
        for (String keyword : depthKeywords) {
            if (answerContent.contains(keyword)) baseScore += 2;
        }
        
        List<String> structureKeywords = Arrays.asList("首先", "其次", "然后", "最后", "总结", "综上", "因此", "所以", "例如", "比如");
        int structureCount = 0;
        for (String keyword : structureKeywords) {
            if (answerContent.contains(keyword)) structureCount++;
        }
        baseScore += Math.min(structureCount * 2, 8);
        
        if (answerContent.contains("```") || answerContent.contains("代码") || answerContent.contains("示例")) {
            baseScore += 5;
        }
        
        if (questionContent.contains("为什么") && answerContent.contains("因为")) baseScore += 3;
        if (questionContent.contains("如何") && (answerContent.contains("步骤") || answerContent.contains("方法"))) baseScore += 3;
        
        if ("100".equals(scoreType)) {
            return Math.min(100, Math.max(0, baseScore));
        } else {
            double fiveScore = baseScore / 20.0;
            return (int) Math.round(Math.min(5, Math.max(1, fiveScore)));
        }
    }

    private String generateFeedback(int score, String scoreType) {
        if ("100".equals(scoreType)) {
            if (score >= 90) {
                return "回答非常优秀，逻辑清晰，内容完整。";
            } else if (score >= 75) {
                return "回答良好，基本覆盖要点，但仍有提升空间。";
            } else if (score >= 60) {
                return "回答及格，需要进一步完善细节。";
            } else {
                return "回答需要改进，建议重新思考问题要点。";
            }
        } else {
            if (score >= 4) {
                return "回答优秀，内容详实。";
            } else if (score >= 3) {
                return "回答良好，继续努力。";
            } else if (score >= 2) {
                return "回答一般，需要加强。";
            } else {
                return "回答有待提高。";
            }
        }
    }
}