package com.jellystudy.controller;

import com.jellystudy.dubbo.AIDubboService;
import com.jellystudy.dubbo.EvaluationDubboService;
import com.jellystudy.dubbo.QuestionDubboService;
import com.jellystudy.entity.Answer;
import com.jellystudy.entity.AnswerEvaluation;
import com.jellystudy.entity.Question;
import com.jellystudy.entity.QuestionEvaluation;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.EvaluationDubboService", timeout = 120000, check = false)
    private EvaluationDubboService evaluationService;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.AIDubboService", timeout = 120000, check = false)
    private AIDubboService aiService;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.QuestionDubboService", timeout = 30000, check = false)
    private QuestionDubboService questionService;

    @PostMapping("/analyze")
    public Map<String, Object> analyzeQuestion(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String questionContent = request.get("questionContent");
        String questionId = request.get("questionId");
        String forceReanalyze = request.getOrDefault("forceReanalyze", "false");

        try {
            QuestionEvaluation evaluation = null;
            
            if ("true".equals(forceReanalyze) || questionId == null || questionId.isEmpty()) {
                evaluation = evaluationService.evaluateQuestion(questionContent);
            } else {
                try {
                    evaluation = evaluationService.findByQuestionId(questionId);
                } catch (Exception e) {
                    // ignore
                }
                if (evaluation == null) {
                    evaluation = evaluationService.evaluateQuestion(questionContent);
                }
            }
            
            if (questionId != null && !questionId.isEmpty()) {
                evaluation.setQuestionId(questionId);
                evaluationService.saveQuestionEvaluation(evaluation);
                
                try {
                    Question question = questionService.findById(questionId);
                    if (question != null) {
                        question.setDifficulty(evaluation.getDifficulty());
                        question.setKnowledgePoints(evaluation.getKnowledgePoints());
                        questionService.update(question);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            
            result.put("success", true);
            result.put("knowledgePoints", evaluation.getKnowledgePoints());
            result.put("difficulty", evaluation.getDifficulty());
            result.put("analysis", generateAnalysisText(evaluation));
            result.put("score", calculateOverallScore(evaluation));
            result.put("evaluateTime", evaluation.getEvaluateTime());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "分析失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String message = request.get("message");

        try {
            String response = aiService.chat(message);
            result.put("success", true);
            result.put("response", response);
        } catch (Exception e) {
            result.put("success", false);
            result.put("response", "抱歉，我暂时无法回答您的问题。错误：" + e.getMessage());
        }

        return result;
    }

    @PostMapping("/analyze-comments")
    public Map<String, Object> analyzeComments(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        List<String> comments = (List<String>) request.get("comments");

        try {
            String analysis = evaluationService.analyzeComments(comments);
            result.put("success", true);
            result.put("analysis", analysis);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "分析失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/analyze-question-ai")
    public Map<String, Object> analyzeQuestionByAI(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String questionContent = request.get("questionContent");

        try {
            String analysis = aiService.analyzeQuestion(questionContent);
            result.put("success", true);
            result.put("analysis", analysis);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AI分析失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/summarize-answer")
    public Map<String, Object> summarizeAnswer(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String answerContent = request.get("answerContent");

        try {
            String summary = aiService.summarizeText(answerContent);
            result.put("success", true);
            result.put("summary", summary);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "总结失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/summary")
    public Map<String, Object> generateSummary(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String type = request.get("type");

        try {
            List<Question> questions = questionService.findAll();

            switch (type) {
                case "questions":
                    result.put("title", "问题数据分析报告");
                    result.put("summary", analyzeQuestions(questions));
                    break;
                case "answers":
                    result.put("title", "回答数据分析报告");
                    result.put("summary", analyzeAnswers(questions));
                    break;
                case "comments":
                    result.put("title", "评论数据分析报告");
                    result.put("summary", analyzeComments(questions));
                    break;
                case "full":
                    result.put("title", "全面数据分析报告");
                    result.put("summary", generateFullReport(questions));
                    break;
            }

            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "生成总结失败: " + e.getMessage());
        }

        return result;
    }

    private String generateAnalysisText(QuestionEvaluation evaluation) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据对问题的分析：\n\n");

        if (evaluation.getKnowledgePoints() != null && !evaluation.getKnowledgePoints().isEmpty()) {
            sb.append("1. 知识点覆盖：本题涉及 ");
            sb.append(String.join("、", evaluation.getKnowledgePoints()));
            sb.append(" 等知识点。\n\n");
        }

        if (evaluation.getDifficulty() != null) {
            sb.append("2. 难度评估：本题属于 ");
            sb.append(evaluation.getDifficulty());
            sb.append(" 难度。\n\n");
        }

        sb.append("3. 建议：建议结合相关知识点进行学习和练习。");
        return sb.toString();
    }

    private int calculateOverallScore(QuestionEvaluation evaluation) {
        if (evaluation.getDifficulty() == null) return 70;

        switch (evaluation.getDifficulty()) {
            case "简单": return 85;
            case "中等": return 70;
            case "困难": return 55;
            default: return 70;
        }
    }

    private String analyzeQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return "目前平台暂无问题数据。";
        }

        int totalQuestions = questions.size();
        int questionsWithAnswers = 0;
        Map<String, Integer> difficultyCount = new HashMap<>();

        for (Question q : questions) {
            if (q.getAnswers() != null && !q.getAnswers().isEmpty()) {
                questionsWithAnswers++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("平台问题现状分析：\n\n");
        sb.append("- 总问题数：").append(totalQuestions).append("个\n");
        sb.append("- 有回答的问题：").append(questionsWithAnswers).append("个\n");
        sb.append("- 问题解答率：").append(String.format("%.1f", (questionsWithAnswers * 100.0 / totalQuestions))).append("%\n\n");

        if (totalQuestions > 0) {
            sb.append("总体来看，平台问题数量适中，建议：\n");
            sb.append("1. 鼓励用户提出更多问题\n");
            sb.append("2. 提高回答的及时性\n");
            sb.append("3. 增强用户互动");
        }

        return sb.toString();
    }

    private String analyzeAnswers(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return "目前平台暂无回答数据。";
        }

        int totalAnswers = 0;
        int totalScore = 0;
        int scoredAnswers = 0;

        for (Question q : questions) {
            if (q.getAnswers() != null) {
                for (Answer a : q.getAnswers()) {
                    totalAnswers++;
                    if (a.getScore() != null) {
                        totalScore += a.getScore();
                        scoredAnswers++;
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("平台回答数据分析：\n\n");
        sb.append("- 总回答数：").append(totalAnswers).append("条\n");
        sb.append("- 已评分回答：").append(scoredAnswers).append("条\n");

        if (scoredAnswers > 0) {
            double avgScore = (double) totalScore / scoredAnswers;
            sb.append("- 平均得分：").append(String.format("%.1f", avgScore)).append("分\n\n");

            if (avgScore >= 80) {
                sb.append("回答质量总体较好！继续保持。");
            } else if (avgScore >= 60) {
                sb.append("回答质量中等，建议：\n");
                sb.append("1. 提供更详细的解释\n");
                sb.append("2. 增加示例代码\n");
                sb.append("3. 提供多种解法");
            } else {
                sb.append("回答质量有待提高，建议：\n");
                sb.append("1. 深入分析问题\n");
                sb.append("2. 提供完整解答\n");
                sb.append("3. 参考优秀答案改进");
            }
        } else {
            sb.append("\n建议启用AI评分功能，提高评估效率。");
        }

        return sb.toString();
    }

    private String analyzeComments(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return "目前平台暂无评论数据。";
        }

        int totalComments = 0;

        for (Question q : questions) {
            if (q.getAnswers() != null) {
                for (Answer a : q.getAnswers()) {
                    if (a.getComments() != null) {
                        totalComments += a.getComments().size();
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("平台评论数据分析：\n\n");
        sb.append("- 总评论数：").append(totalComments).append("条\n\n");

        if (totalComments > 0) {
            sb.append("评论互动较为活跃。良好的评论互动有助于：\n");
            sb.append("1. 深入讨论问题\n");
            sb.append("2. 纠正错误观点\n");
            sb.append("3. 提供额外见解");
        } else {
            sb.append("评论互动较少，建议：\n");
            sb.append("1. 鼓励用户评论讨论\n");
            sb.append("2. 在回答下添加追问功能\n");
            sb.append("3. 展示热门评论");
        }

        return sb.toString();
    }

    private String generateFullReport(List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("         JellyStudy 平台数据总览\n");
        sb.append("========================================\n\n");

        if (questions == null || questions.isEmpty()) {
            sb.append("平台暂无数据，请先添加一些问题。");
            return sb.toString();
        }

        int totalQuestions = questions.size();
        int totalAnswers = 0;
        int totalComments = 0;
        int totalLikes = 0;
        int evaluatedAnswers = 0;
        int totalScore = 0;

        for (Question q : questions) {
            totalLikes += q.getLikeCount();
            if (q.getAnswers() != null) {
                for (Answer a : q.getAnswers()) {
                    totalAnswers++;
                    totalLikes += a.getLikeCount();
                    if (a.getComments() != null) {
                        totalComments += a.getComments().size();
                    }
                    if (a.getScore() != null) {
                        evaluatedAnswers++;
                        totalScore += a.getScore();
                    }
                }
            }
        }

        sb.append("【内容统计】\n");
        sb.append("- 问题总数：").append(totalQuestions).append("\n");
        sb.append("- 回答总数：").append(totalAnswers).append("\n");
        sb.append("- 评论总数：").append(totalComments).append("\n");
        sb.append("- 获赞总数：").append(totalLikes).append("\n\n");

        sb.append("【互动分析】\n");
        double avgAnswers = totalQuestions > 0 ? (double) totalAnswers / totalQuestions : 0;
        double avgComments = totalAnswers > 0 ? (double) totalComments / totalAnswers : 0;
        sb.append("- 平均每题回答数：").append(String.format("%.1f", avgAnswers)).append("\n");
        sb.append("- 平均每回答评论数：").append(String.format("%.1f", avgComments)).append("\n\n");

        sb.append("【AI评估】\n");
        if (evaluatedAnswers > 0) {
            double avgScore = (double) totalScore / evaluatedAnswers;
            sb.append("- 已评估回答：").append(evaluatedAnswers).append("\n");
            sb.append("- 平均得分：").append(String.format("%.1f", avgScore)).append("分\n\n");

            if (avgScore >= 80) {
                sb.append("【评价】平台内容质量优秀！");
            } else if (avgScore >= 60) {
                sb.append("【评价】平台内容质量良好，仍有提升空间。");
            } else {
                sb.append("【评价】建议优化回答质量，鼓励更优质的内容。");
            }
        } else {
            sb.append("- 暂无评估数据\n\n");
            sb.append("建议：启用AI评分功能，提高内容质量把控。");
        }

        sb.append("\n========================================\n");

        return sb.toString();
    }

    @PostMapping("/ai-analyze-question")
    public Map<String, Object> aiAnalyzeQuestion(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String questionContent = request.get("questionContent");
        String questionId = request.get("questionId");

        try {
            if (questionId != null && !questionId.isEmpty()) {
                try {
                    Question q = questionService.findById(questionId);
                    if (q != null) {
                        questionContent = q.getTitle() + "\n" + q.getContent();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }

            if (questionContent == null || questionContent.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "问题内容不能为空");
                return result;
            }

            String aiAnalysis = aiService.analyzeQuestion(questionContent);

            QuestionEvaluation eval = null;
            try {
                eval = evaluationService.evaluateQuestion(questionContent);
            } catch (Exception e) {
                // ignore
            }

            result.put("success", true);
            result.put("analysis", aiAnalysis);
            if (eval != null) {
                result.put("knowledgePoints", eval.getKnowledgePoints());
                result.put("difficulty", eval.getDifficulty());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AI分析失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/ai-database-summary")
    public Map<String, Object> aiDatabaseSummary(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String type = request.getOrDefault("type", "full");

        try {
            List<Question> questions = questionService.findAll();

            if (questions == null || questions.isEmpty()) {
                result.put("success", false);
                result.put("message", "平台暂无数据，请先添加一些问题");
                return result;
            }

            StringBuilder dataText = new StringBuilder();

            switch (type) {
                case "questions":
                    dataText.append("以下是平台上的所有问题：\n\n");
                    for (int i = 0; i < questions.size(); i++) {
                        Question q = questions.get(i);
                        dataText.append(i + 1).append(". ").append(q.getTitle());
                        if (q.getDifficulty() != null) dataText.append(" [").append(q.getDifficulty()).append("]");
                        dataText.append("\n");
                    }
                    break;
                case "answers":
                    dataText.append("以下是平台上所有问题的回答数据：\n\n");
                    for (Question q : questions) {
                        if (q.getAnswers() != null && !q.getAnswers().isEmpty()) {
                            dataText.append("问题：").append(q.getTitle()).append("\n");
                            for (Answer a : q.getAnswers()) {
                                dataText.append("  - 回答(by ").append(a.getAuthor()).append(")");
                                if (a.getScore() != null) dataText.append(" [AI评分:").append(a.getScore()).append("分]");
                                dataText.append(": ").append(a.getContent().length() > 100 ? a.getContent().substring(0, 100) + "..." : a.getContent());
                                dataText.append("\n");
                            }
                        }
                    }
                    break;
                case "comments":
                    dataText.append("以下是平台上所有的评论数据：\n\n");
                    for (Question q : questions) {
                        if (q.getAnswers() != null) {
                            for (Answer a : q.getAnswers()) {
                                if (a.getComments() != null && !a.getComments().isEmpty()) {
                                    dataText.append("问题：").append(q.getTitle()).append("\n");
                                    for (com.jellystudy.entity.Comment c : a.getComments()) {
                                        dataText.append("  - 评论(by ").append(c.getAuthor()).append(")");
                                        if (c.getScore() != null) dataText.append(" [AI评分:").append(c.getScore()).append("分]");
                                        dataText.append(": ").append(c.getContent()).append("\n");
                                    }
                                }
                            }
                        }
                    }
                    break;
                case "full":
                default:
                    int totalAnswers = 0, totalComments = 0, totalLikes = 0;
                    int scoredAnswers = 0, totalScore = 0;
                    Map<String, Integer> difficultyMap = new HashMap<>();
                    Map<String, Integer> kpMap = new HashMap<>();

                    for (Question q : questions) {
                        totalLikes += q.getLikeCount();
                        if (q.getDifficulty() != null) {
                            difficultyMap.merge(q.getDifficulty(), 1, Integer::sum);
                        }
                        if (q.getKnowledgePoints() != null) {
                            for (String kp : q.getKnowledgePoints()) {
                                kpMap.merge(kp, 1, Integer::sum);
                            }
                        }
                        if (q.getAnswers() != null) {
                            for (Answer a : q.getAnswers()) {
                                totalAnswers++;
                                totalLikes += a.getLikeCount();
                                if (a.getScore() != null) {
                                    scoredAnswers++;
                                    totalScore += a.getScore();
                                }
                                if (a.getComments() != null) {
                                    totalComments += a.getComments().size();
                                }
                            }
                        }
                    }

                    dataText.append("JellyStudy平台全面数据分析：\n\n");
                    dataText.append("【基本统计】\n");
                    dataText.append("- 问题总数：").append(questions.size()).append("\n");
                    dataText.append("- 回答总数：").append(totalAnswers).append("\n");
                    dataText.append("- 评论总数：").append(totalComments).append("\n");
                    dataText.append("- 点赞总数：").append(totalLikes).append("\n\n");

                    dataText.append("【难度分布】\n");
                    for (Map.Entry<String, Integer> e : difficultyMap.entrySet()) {
                        dataText.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("个\n");
                    }
                    dataText.append("\n");

                    dataText.append("【热门知识点TOP10】\n");
                    kpMap.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .limit(10)
                            .forEach(e -> dataText.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("个问题\n"));
                    dataText.append("\n");

                    if (scoredAnswers > 0) {
                        dataText.append("【AI评分统计】\n");
                        dataText.append("- 已评分回答：").append(scoredAnswers).append("条\n");
                        dataText.append("- 平均得分：").append(String.format("%.1f", (double) totalScore / scoredAnswers)).append("分\n");
                    }

                    dataText.append("\n【问题列表】\n");
                    for (int i = 0; i < Math.min(questions.size(), 20); i++) {
                        Question q = questions.get(i);
                        dataText.append(i + 1).append(". ").append(q.getTitle());
                        if (q.getDifficulty() != null) dataText.append(" [").append(q.getDifficulty()).append("]");
                        int ansCount = q.getAnswers() != null ? q.getAnswers().size() : 0;
                        dataText.append(" (").append(ansCount).append("个回答)\n");
                    }
                    break;
            }

            String aiSummary = aiService.summarizeText(dataText.toString());

            result.put("success", true);
            result.put("summary", aiSummary);
            result.put("type", type);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AI总结失败: " + e.getMessage());
        }

        return result;
    }
}
