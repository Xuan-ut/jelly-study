package com.jellystudy.service;

import com.jellystudy.dubbo.EvaluationDubboService;
import com.jellystudy.dubbo.KnowledgePointDubboService;
import com.jellystudy.dubbo.QuestionDubboService;
import com.jellystudy.entity.Answer;
import com.jellystudy.entity.AnswerEvaluation;
import com.jellystudy.entity.Comment;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.entity.Question;
import com.jellystudy.entity.QuestionEvaluation;
import com.jellystudy.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.QuestionDubboService")
public class QuestionServiceImpl implements QuestionDubboService {

    @Autowired
    private QuestionRepository repository;
    
    @Autowired
    private RedisService redisService;
    
    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.EvaluationDubboService", timeout = 120000, check = false)
    private EvaluationDubboService evaluationService;
    
    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.KnowledgePointDubboService", timeout = 30000, check = false)
    private KnowledgePointDubboService knowledgePointService;

    @Override
    public Question create(Question question) {
        question.setCreateTime(new Date());
        question.setUpdateTime(new Date());
        
        boolean hasKnowledgePoints = (question.getKnowledgePoints() != null && !question.getKnowledgePoints().isEmpty())
            || (question.getKnowledgePointIds() != null && !question.getKnowledgePointIds().isEmpty());
        
        if (hasKnowledgePoints) {
            if (question.getDifficulty() == null || question.getDifficulty().isEmpty()) {
                question.setDifficulty(evaluateDifficultyLocally(question.getContent()));
            }
            validateAndCreateKnowledgePoints(question);
            return repository.save(question);
        }
        
        try {
            QuestionEvaluation evaluation = evaluationService.evaluateQuestion(question.getContent());
            question.setDifficulty(evaluation.getDifficulty());
            question.setKnowledgePoints(evaluation.getKnowledgePoints());
            
            Question savedQuestion = repository.save(question);
            evaluation.setQuestionId(savedQuestion.getId());
            evaluationService.saveQuestionEvaluation(evaluation);
            
            validateAndCreateKnowledgePoints(question);
            return savedQuestion;
        } catch (Throwable e) {
            log.warn("评估问题失败: {}", e.getMessage());
            question.setDifficulty(evaluateDifficultyLocally(question.getContent()));
            validateAndCreateKnowledgePoints(question);
            return repository.save(question);
        }
    }
    
    private String evaluateDifficultyLocally(String content) {
        if (content == null) return "中等";
        int score = 0;
        String[] hardKeywords = {"架构", "分布式", "高并发", "JVM", "微服务", "集群", "性能优化", "源码"};
        String[] mediumKeywords = {"Spring", "事务", "缓存", "设计模式", "数据库", "并发", "线程"};
        String[] easyKeywords = {"基础", "入门", "语法", "简单", "基本"};
        for (String kw : hardKeywords) { if (content.contains(kw)) score += 3; }
        for (String kw : mediumKeywords) { if (content.contains(kw)) score += 2; }
        for (String kw : easyKeywords) { if (content.contains(kw)) score -= 1; }
        if (content.length() > 300) score += 2;
        else if (content.length() > 150) score += 1;
        if (score >= 5) return "困难";
        if (score >= 2) return "中等";
        return "简单";
    }

    @Override
    public Question findById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<Question> findAll() {
        return repository.findAll();
    }

    @Override
    public Page<Question> findByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return repository.findAll(pageable);
    }

    @Override
    public Question update(Question question) {
        question.setUpdateTime(new Date());
        return repository.save(question);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public Question addAnswer(String questionId, Answer answer) {
        Question question = findById(questionId);
        if (question != null) {
            answer.setId(UUID.randomUUID().toString());
            answer.setCreateTime(new Date());
            answer.setUpdateTime(new Date());
            
            try {
                AnswerEvaluation evaluation = evaluationService.evaluateAnswer(question.getContent(), answer.getContent(), "100");
                answer.setScore(evaluation.getScore());
                answer.setFeedback(evaluation.getFeedback());
                evaluation.setQuestionId(questionId);
                evaluation.setAnswerId(answer.getId());
                evaluationService.saveAnswerEvaluation(evaluation);
            } catch (Throwable e) {
                log.warn("评估回答失败: {}", e.getMessage());
            }
            
            if (question.getAnswers() == null) {
                question.setAnswers(new java.util.ArrayList<>());
            }
            question.getAnswers().add(answer);
            question.setAnswerCount(question.getAnswerCount() + 1);
            question.setUpdateTime(new Date());
            Question savedQuestion = repository.save(question);
            
            // 更新Redis中的受欢迎度分数（回答数增加）
            try {
                int commentCount = question.getAnswers().stream()
                        .mapToInt(a -> a.getComments() != null ? a.getComments().size() : 0)
                        .sum();
                redisService.updatePopularScore(questionId, question.getLikeCount(),
                        question.getAnswers().size(), commentCount, question.getCreateTime());
                log.debug("问题[{}]添加回答后更新Redis受欢迎度分数", questionId);
            } catch (Exception e) {
                log.warn("更新Redis受欢迎度分数失败: {}", e.getMessage());
            }
            
            return savedQuestion;
        }
        return null;
    }

    @Override
    public Question addComment(String questionId, String answerId, Comment comment) {
        Question question = findById(questionId);
        if (question != null) {
            for (Answer answer : question.getAnswers()) {
                if (answer.getId().equals(answerId)) {
                    comment.setId(UUID.randomUUID().toString());
                    comment.setCreateTime(new Date());
                    comment.setUpdateTime(new Date());
                    
                    try {
                        Map<String, Object> evalResult = evaluationService.evaluateComment(question.getContent(), comment.getContent());
                        if (evalResult != null) {
                            Object scoreObj = evalResult.get("score");
                            if (scoreObj != null) {
                                comment.setScore(((Number) scoreObj).intValue());
                            }
                            Object feedbackObj = evalResult.get("feedback");
                            if (feedbackObj != null) {
                                comment.setFeedback(String.valueOf(feedbackObj));
                            }
                            log.info("评论AI评估完成: score={}", evalResult.get("score"));
                        }
                    } catch (Throwable e) {
                        log.warn("评论AI评估失败: {}", e.getMessage());
                    }
                    
                    if (answer.getComments() == null) {
                        answer.setComments(new java.util.ArrayList<>());
                    }
                    answer.getComments().add(comment);
                    answer.setUpdateTime(new Date());
                    question.setUpdateTime(new Date());
                    Question savedQuestion = repository.save(question);
                    
                    // 更新Redis中的受欢迎度分数（评论数增加）
                    try {
                        int commentCount = question.getAnswers().stream()
                                .mapToInt(a -> a.getComments() != null ? a.getComments().size() : 0)
                                .sum();
                        redisService.updatePopularScore(questionId, question.getLikeCount(),
                                question.getAnswers() != null ? question.getAnswers().size() : 0,
                                commentCount, question.getCreateTime());
                        log.debug("问题[{}]添加评论后更新Redis受欢迎度分数", questionId);
                    } catch (Exception e) {
                        log.warn("更新Redis受欢迎度分数失败: {}", e.getMessage());
                    }
                    
                    return savedQuestion;
                }
            }
        }
        return null;
    }

    @Override
    public Question like(String questionId) {
        Question question = findById(questionId);
        if (question != null) {
            question.setLikeCount(question.getLikeCount() + 1);
            question.setUpdateTime(new Date());
            Question savedQuestion = repository.save(question);
            
            // 更新Redis中的受欢迎度分数
            try {
                int commentCount = question.getAnswers().stream()
                        .mapToInt(a -> a.getComments() != null ? a.getComments().size() : 0)
                        .sum();
                redisService.updatePopularScore(questionId, question.getLikeCount(),
                        question.getAnswers() != null ? question.getAnswers().size() : 0,
                        commentCount, question.getCreateTime());
                log.debug("更新问题[{}]的Redis受欢迎度分数", questionId);
            } catch (Exception e) {
                log.warn("更新Redis受欢迎度分数失败: {}", e.getMessage());
            }
            
            return savedQuestion;
        }
        return null;
    }

    @Override
    public Answer likeAnswer(String questionId, String answerId) {
        Question question = findById(questionId);
        if (question != null) {
            for (Answer answer : question.getAnswers()) {
                if (answer.getId().equals(answerId)) {
                    answer.setLikeCount(answer.getLikeCount() + 1);
                    answer.setUpdateTime(new Date());
                    question.setUpdateTime(new Date());
                    repository.save(question);
                    return answer;
                }
            }
        }
        return null;
    }

    @Override
    public Comment likeComment(String questionId, String answerId, String commentId) {
        Question question = findById(questionId);
        if (question != null) {
            for (Answer answer : question.getAnswers()) {
                if (answer.getId().equals(answerId)) {
                    for (Comment comment : answer.getComments()) {
                        if (comment.getId().equals(commentId)) {
                            comment.setLikeCount(comment.getLikeCount() + 1);
                            comment.setUpdateTime(new Date());
                            answer.setUpdateTime(new Date());
                            question.setUpdateTime(new Date());
                            repository.save(question);
                            return comment;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<Question> getHotQuestions() {
        // 优先从Redis缓存获取热点问题（主要基于点击量，结合点赞量）
        try {
            List<Question> hotQuestions = redisService.getHotQuestionsWithViewAndLike(10);
            if (hotQuestions != null && !hotQuestions.isEmpty()) {
                log.debug("从Redis缓存获取热点问题成功，共{}个", hotQuestions.size());
                return hotQuestions;
            }
        } catch (Exception e) {
            log.warn("从Redis获取热点问题失败，降级到数据库查询: {}", e.getMessage());
        }
        
        // 如果Redis不可用或缓存为空，从数据库查询（基于点赞量排序）
        List<Question> dbQuestions = repository.findTop10ByOrderByLikeCountDesc();
        
        // 尝试将数据库查询结果预热到Redis
        try {
            for (Question q : dbQuestions) {
                int commentCount = q.getAnswers() != null ? 
                        q.getAnswers().stream()
                                .mapToInt(a -> a.getComments() != null ? a.getComments().size() : 0)
                                .sum() : 0;
                redisService.updatePopularScore(q.getId(), q.getLikeCount(),
                        q.getAnswers() != null ? q.getAnswers().size() : 0,
                        commentCount, q.getCreateTime());
            }
            log.debug("热点问题数据已预热到Redis");
        } catch (Exception e) {
            log.warn("预热热点问题到Redis失败: {}", e.getMessage());
        }
        
        return dbQuestions;
    }

    @Override
    public List<Question> getRecommendedQuestions() {
        return repository.findTop10ByOrderByAnswerCountDesc();
    }
    
    /**
     * 获取推荐问题（基于知识点关联推荐）
     * 调用 KnowledgePointService 获取相关知识点，然后推荐关联问题
     */
    public List<Question> getRecommendedQuestionsByKnowledge(String questionId) {
        Question question = findById(questionId);
        if (question == null || question.getKnowledgePoints() == null || question.getKnowledgePoints().isEmpty()) {
            return getRecommendedQuestions();
        }
        
        // 调用知识点服务获取相关知识点详情
        List<Question> recommended = new java.util.ArrayList<>();
        for (String knowledgePointName : question.getKnowledgePoints()) {
            try {
                KnowledgePoint kp = knowledgePointService.findByName(knowledgePointName);
                if (kp != null) {
                    List<Question> relatedQuestions = getQuestionsByKnowledgePoint(kp.getId());
                    for (Question q : relatedQuestions) {
                        if (!q.getId().equals(questionId) && 
                            !recommended.stream().anyMatch(r -> r.getId().equals(q.getId()))) {
                            recommended.add(q);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取知识点[{}]相关问题失败: {}", knowledgePointName, e.getMessage());
            }
        }
        
        // 如果没有找到关联问题，返回默认推荐
        if (recommended.isEmpty()) {
            return getRecommendedQuestions();
        }
        
        return recommended.size() > 10 ? recommended.subList(0, 10) : recommended;
    }
    
    /**
     * 创建问题时验证知识点
     * 调用 KnowledgePointService 检查知识点是否存在，不存在则创建
     */
    private void validateAndCreateKnowledgePoints(Question question) {
        if (question.getKnowledgePoints() == null || question.getKnowledgePoints().isEmpty()) {
            return;
        }
        
        if (question.getKnowledgePointIds() == null) {
            question.setKnowledgePointIds(new java.util.ArrayList<>());
        }
        
        for (String kpName : question.getKnowledgePoints()) {
            try {
                KnowledgePoint existing = knowledgePointService.findByName(kpName);
                if (existing == null) {
                    KnowledgePoint newKP = new KnowledgePoint();
                    newKP.setName(kpName);
                    newKP.setDescription("自动创建的知识点: " + kpName);
                    existing = knowledgePointService.create(newKP);
                    if (!question.getKnowledgePointIds().contains(existing.getId())) {
                        question.getKnowledgePointIds().add(existing.getId());
                    }
                    log.info("自动创建知识点: {}", kpName);
                } else {
                    if (!question.getKnowledgePointIds().contains(existing.getId())) {
                        question.getKnowledgePointIds().add(existing.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("验证知识点[{}]失败: {}", kpName, e.getMessage());
            }
        }
    }

    @Override
    public List<Question> getQuestionsByKnowledgePoint(String knowledgePointId) {
        return repository.findByKnowledgePointIdsContaining(knowledgePointId);
    }

    @Override
    public List<Question> searchQuestions(String keyword) {
        List<Question> titleResults = repository.findByTitleContaining(keyword);
        List<Question> contentResults = repository.findByContentContaining(keyword);

        List<Question> combined = new java.util.ArrayList<>();
        combined.addAll(titleResults);

        for (Question q : contentResults) {
            boolean exists = combined.stream().anyMatch(existing -> existing.getId().equals(q.getId()));
            if (!exists) {
                combined.add(q);
            }
        }

        return combined;
    }

    @Override
    public long count() {
        return repository.count();
    }
}