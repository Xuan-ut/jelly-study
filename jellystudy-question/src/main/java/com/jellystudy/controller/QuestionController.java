package com.jellystudy.controller;

import com.jellystudy.dubbo.EvaluationDubboService;
import com.jellystudy.dubbo.KnowledgePointDubboService;
import com.jellystudy.dubbo.QuestionDubboService;
import com.jellystudy.entity.Answer;
import com.jellystudy.entity.Comment;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.entity.Question;
import com.jellystudy.entity.QuestionEvaluation;
import com.jellystudy.repository.QuestionRepository;
import com.jellystudy.service.QuestionServiceImpl;
import com.jellystudy.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    @Autowired
    private QuestionServiceImpl questionService;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.EvaluationDubboService", timeout = 120000, check = false)
    private EvaluationDubboService evaluationService;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.KnowledgePointDubboService", timeout = 30000, check = false)
    private KnowledgePointDubboService knowledgePointService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private RedisService redisService;

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            Question question = new Question();
            question.setTitle((String) body.get("title"));
            question.setContent((String) body.get("content"));
            question.setAuthor((String) body.get("author"));
            question.setCreateTime(new Date());
            question.setUpdateTime(new Date());
            
            List<String> knowledgePointIds = new java.util.ArrayList<>();
            Object kpIdsObj = body.get("knowledgePointIds");
            if (kpIdsObj instanceof List) {
                for (Object item : (List<?>) kpIdsObj) {
                    knowledgePointIds.add(String.valueOf(item));
                }
            }
            
            List<String> knowledgePoints = new java.util.ArrayList<>();
            Object kpsObj = body.get("knowledgePoints");
            if (kpsObj instanceof List) {
                for (Object item : (List<?>) kpsObj) {
                    knowledgePoints.add(String.valueOf(item));
                }
            }
            
            Map<String, String> descriptions = new java.util.HashMap<>();
            Object descObj = body.get("knowledgePointDescriptions");
            if (descObj instanceof Map) {
                Map<?, ?> descMap = (Map<?, ?>) descObj;
                for (Map.Entry<?, ?> entry : descMap.entrySet()) {
                    descriptions.put(String.valueOf(entry.getKey()), 
                                     entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
                }
            }
            
            String difficulty = body.get("difficulty") != null ? String.valueOf(body.get("difficulty")) : null;
            
            for (String kpName : knowledgePoints) {
                try {
                    KnowledgePoint existing = knowledgePointService.findByName(kpName);
                    if (existing == null) {
                        KnowledgePoint newKP = new KnowledgePoint();
                        newKP.setName(kpName);
                        String desc = descriptions.get(kpName);
                        newKP.setDescription(desc != null && !desc.trim().isEmpty() ? desc : "AI提取的知识点: " + kpName);
                        knowledgePointService.create(newKP);
                        log.info("预创建知识点: {}, 描述: {}", kpName, desc);
                    }
                } catch (Exception e) {
                    log.warn("创建知识点[{}]失败: {}", kpName, e.getMessage());
                }
            }
            
            List<String> allKPNames = new java.util.ArrayList<>(knowledgePoints);
            for (String kpId : knowledgePointIds) {
                try {
                    KnowledgePoint kp = knowledgePointService.findById(kpId);
                    if (kp != null && !allKPNames.contains(kp.getName())) {
                        allKPNames.add(kp.getName());
                    }
                } catch (Exception e) {
                    log.warn("查询知识点[{}]失败: {}", kpId, e.getMessage());
                }
            }
            
            question.setKnowledgePointIds(knowledgePointIds);
            question.setKnowledgePoints(allKPNames);
            question.setDifficulty(difficulty);
            
            Question saved = questionService.create(question);
            result.put("success", true);
            result.put("id", saved.getId());
            result.put("data", saved);
        } catch (Throwable e) {
            log.error("创建问题失败", e);
            result.put("success", false);
            result.put("message", "创建失败: " + e.getMessage());
            result.put("exception", e.getClass().getName());
            if (e.getCause() != null) {
                result.put("cause", e.getCause().getMessage());
            }
        }
        return result;
    }

    @GetMapping
    public List<Question> findAll() {
        return questionService.findAll();
    }

    @GetMapping("/{id}")
    public Map<String, Object> findById(@PathVariable String id, HttpServletRequest request) {
        redisService.recordQuestionView(id);
        String sessionId = getSessionId(request);
        redisService.recordBrowseHistory(sessionId, id);
        
        Question question = questionService.findById(id);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("question", question);
        
        try {
            QuestionEvaluation evaluation = evaluationService.getQuestionEvaluation(id);
            result.put("evaluation", evaluation);
            log.debug("获取问题[{}]的评估信息: {}", id, evaluation != null ? "命中" : "未命中");
        } catch (Exception e) {
            log.warn("调用EvaluationService获取评估信息失败: {}", e.getMessage());
            result.put("evaluation", null);
        }
        
        if (question != null && question.getKnowledgePoints() != null && !question.getKnowledgePoints().isEmpty()) {
            try {
                Map<String, KnowledgePoint> kpDetails = new java.util.HashMap<>();
                for (String kpName : question.getKnowledgePoints()) {
                    KnowledgePoint kp = knowledgePointService.findByName(kpName);
                    if (kp != null) {
                        kpDetails.put(kpName, kp);
                    }
                }
                result.put("knowledgePointDetails", kpDetails);
                log.debug("获取问题[{}]的{}个知识点详情", id, kpDetails.size());
            } catch (Exception e) {
                log.warn("调用KnowledgePointService获取知识点详情失败: {}", e.getMessage());
                result.put("knowledgePointDetails", null);
            }
        }
        
        return result;
    }

    @PutMapping
    public Question update(@RequestBody Question question) {
        question.setUpdateTime(new Date());
        return questionService.update(question);
    }

    @PutMapping("/{id}/analysis")
    public Question updateAnalysis(@PathVariable String id, @RequestBody Map<String, Object> analysisData) {
        Question question = questionRepository.findById(id).orElse(null);
        if (question != null) {
            if (analysisData.containsKey("difficulty")) {
                question.setDifficulty((String) analysisData.get("difficulty"));
            }
            if (analysisData.containsKey("knowledgePoints")) {
                question.setKnowledgePoints((List<String>) analysisData.get("knowledgePoints"));
            }
            question.setUpdateTime(new Date());
            questionRepository.save(question);
        }
        return question;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        questionService.delete(id);
    }

    @PostMapping("/{questionId}/answers")
    public Question addAnswer(@PathVariable String questionId, @RequestBody Answer answer) {
        return questionService.addAnswer(questionId, answer);
    }

    @PostMapping("/{questionId}/answers/{answerId}/comments")
    public Question addComment(@PathVariable String questionId, @PathVariable String answerId, @RequestBody Comment comment) {
        return questionService.addComment(questionId, answerId, comment);
    }

    @PostMapping("/{questionId}/like")
    public Question likeQuestion(@PathVariable String questionId) {
        return questionService.like(questionId);
    }

    @PostMapping("/{questionId}/answers/{answerId}/like")
    public Question likeAnswer(@PathVariable String questionId, @PathVariable String answerId) {
        questionService.likeAnswer(questionId, answerId);
        return questionService.findById(questionId);
    }

    @PostMapping("/{questionId}/answers/{answerId}/comments/{commentId}/like")
    public Question likeComment(@PathVariable String questionId, @PathVariable String answerId, @PathVariable String commentId) {
        questionService.likeComment(questionId, answerId, commentId);
        return questionService.findById(questionId);
    }

    @GetMapping("/count")
    public long countQuestions() {
        return questionService.count();
    }

    @GetMapping("/hot")
    public List<Question> getHotQuestions() {
        return questionService.getHotQuestions();
    }

    @GetMapping("/recommended")
    public List<Question> getRecommendedQuestions() {
        return questionService.getRecommendedQuestions();
    }

    @GetMapping("/recommended/{questionId}")
    public Map<String, Object> getRecommendedByKnowledge(@PathVariable String questionId) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            com.jellystudy.service.QuestionServiceImpl questionServiceImpl = 
                (com.jellystudy.service.QuestionServiceImpl) questionService;
            List<Question> recommended = questionServiceImpl.getRecommendedQuestionsByKnowledge(questionId);
            result.put("success", true);
            result.put("data", recommended);
            result.put("count", recommended.size());
            log.info("获取问题[{}]的知识点关联推荐，共{}个", questionId, recommended.size());
        } catch (Exception e) {
            log.warn("获取知识点关联推荐失败: {}", e.getMessage());
            result.put("success", false);
            result.put("data", questionService.getRecommendedQuestions());
            result.put("message", "降级为默认推荐");
        }
        return result;
    }

    @GetMapping("/knowledge-point/{knowledgePointId}")
    public List<Question> getQuestionsByKnowledgePoint(@PathVariable String knowledgePointId) {
        return questionService.getQuestionsByKnowledgePoint(knowledgePointId);
    }

    @GetMapping("/search")
    public List<Question> searchQuestions(@RequestParam String keyword) {
        return questionService.searchQuestions(keyword);
    }

    @GetMapping("/page")
    public Map<String, Object> getQuestionsByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Question> questionPage = questionRepository.findAll(pageable);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", questionPage.getContent());
        result.put("number", questionPage.getNumber());
        result.put("size", questionPage.getSize());
        result.put("totalElements", questionPage.getTotalElements());
        result.put("totalPages", questionPage.getTotalPages());
        return result;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new java.util.HashMap<>();
        
        long totalQuestions = questionRepository.count();
        result.put("totalQuestions", totalQuestions);
        
        int totalAnswers = 0;
        int evaluatedAnswers = 0;
        int excellentAnswers = 0;
        int totalScore = 0;
        int scoreCount = 0;
        
        int easyCount = 0;
        int mediumCount = 0;
        int hardCount = 0;
        
        List<Question> questions = questionRepository.findAll();
        for (Question q : questions) {
            if (q.getAnswers() != null) {
                totalAnswers += q.getAnswers().size();
                for (Answer a : q.getAnswers()) {
                    if (a.getScore() != null) {
                        evaluatedAnswers++;
                        totalScore += a.getScore();
                        scoreCount++;
                        if (a.getScore() >= 80) {
                            excellentAnswers++;
                        }
                    }
                }
            }
            
            if ("简单".equals(q.getDifficulty())) {
                easyCount++;
            } else if ("中等".equals(q.getDifficulty())) {
                mediumCount++;
            } else if ("困难".equals(q.getDifficulty())) {
                hardCount++;
            }
        }
        
        result.put("totalAnswers", totalAnswers);
        result.put("evaluatedAnswers", evaluatedAnswers);
        result.put("excellentAnswers", excellentAnswers);
        
        if (scoreCount > 0) {
            result.put("avgScore", totalScore / scoreCount);
        } else {
            result.put("avgScore", 0);
        }
        
        Map<String, Integer> difficultyStats = new java.util.HashMap<>();
        difficultyStats.put("easy", easyCount);
        difficultyStats.put("medium", mediumCount);
        difficultyStats.put("hard", hardCount);
        result.put("difficultyStats", difficultyStats);
        
        int questionEvaluations = easyCount + mediumCount + hardCount;
        result.put("questionEvaluations", questionEvaluations);
        result.put("answerEvaluations", evaluatedAnswers);
        
        return result;
    }

    @GetMapping("/count-by-knowledge-points")
    public Map<String, Integer> getQuestionCountByKnowledgePoints() {
        Map<String, Integer> countMap = new java.util.HashMap<>();
        List<Question> questions = questionRepository.findAll();
        
        for (Question q : questions) {
            List<String> knowledgePointIds = q.getKnowledgePointIds();
            if (knowledgePointIds != null) {
                for (String kpId : knowledgePointIds) {
                    countMap.put(kpId, countMap.getOrDefault(kpId, 0) + 1);
                }
            }
        }
        
        return countMap;
    }

    @GetMapping("/history")
    public List<Map<String, Object>> getBrowseHistory(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        return redisService.getBrowseHistory(sessionId);
    }

    @DeleteMapping("/history")
    public Map<String, Object> clearBrowseHistory(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        redisService.clearBrowseHistory(sessionId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "浏览历史已清除");
        return result;
    }

    @PostMapping("/{id}/link-knowledge-points")
    public Map<String, Object> linkKnowledgePoints(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            List<String> knowledgePointNames = (List<String>) body.get("knowledgePoints");
            Question question = questionService.findById(id);
            if (question == null) {
                result.put("success", false);
                result.put("message", "问题不存在");
                return result;
            }
            
            List<String> existingKPs = question.getKnowledgePoints();
            if (existingKPs == null) {
                existingKPs = new java.util.ArrayList<>();
            }
            
            List<String> existingKPIds = question.getKnowledgePointIds();
            if (existingKPIds == null) {
                existingKPIds = new java.util.ArrayList<>();
            }
            
            for (String kpName : knowledgePointNames) {
                if (!existingKPs.contains(kpName)) {
                    existingKPs.add(kpName);
                }
                try {
                    KnowledgePoint existing = knowledgePointService.findByName(kpName);
                    if (existing == null) {
                        KnowledgePoint newKP = new KnowledgePoint();
                        newKP.setName(kpName);
                        newKP.setDescription("AI提取的知识点: " + kpName);
                        existing = knowledgePointService.create(newKP);
                        existingKPIds.add(existing.getId());
                        log.info("创建新知识点: {}", kpName);
                    } else {
                        if (!existingKPIds.contains(existing.getId())) {
                            existingKPIds.add(existing.getId());
                        }
                    }
                } catch (Exception e) {
                    log.warn("创建知识点[{}]失败: {}", kpName, e.getMessage());
                }
            }
            
            question.setKnowledgePoints(existingKPs);
            question.setKnowledgePointIds(existingKPIds);
            question.setUpdateTime(new Date());
            questionService.update(question);
            
            result.put("success", true);
            result.put("message", "知识点关联成功");
            result.put("knowledgePoints", existingKPs);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "关联失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/analyze-knowledge-points")
    public Map<String, Object> analyzeKnowledgePoints(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            String content = body.get("content");
            if (content == null || content.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "内容不能为空");
                return result;
            }
            
            QuestionEvaluation evaluation = evaluationService.evaluateQuestion(content);
            List<String> knowledgePoints = evaluation.getKnowledgePoints();
            String difficulty = evaluation.getDifficulty();
            
            result.put("success", true);
            result.put("knowledgePoints", knowledgePoints);
            result.put("difficulty", difficulty);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "分析失败: " + e.getMessage());
        }
        return result;
    }

    private String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        return session.getId();
    }
}
