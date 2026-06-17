package com.jellystudy.dubbo;

import com.jellystudy.entity.AnswerEvaluation;
import com.jellystudy.entity.QuestionEvaluation;

import java.util.List;
import java.util.Map;

public interface EvaluationDubboService {
    QuestionEvaluation evaluateQuestion(String questionContent);
    
    AnswerEvaluation evaluateAnswer(String questionContent, String answerContent, String scoreType);
    
    Map<String, Object> evaluateComment(String questionContent, String commentContent);
    
    QuestionEvaluation findByQuestionId(String questionId);
    
    QuestionEvaluation saveQuestionEvaluation(QuestionEvaluation evaluation);
    
    AnswerEvaluation saveAnswerEvaluation(AnswerEvaluation evaluation);
    
    QuestionEvaluation getQuestionEvaluation(String questionId);
    
    AnswerEvaluation getAnswerEvaluation(String answerId);
    
    List<QuestionEvaluation> getQuestionEvaluationsByQuestionIds(List<String> questionIds);
    
    List<AnswerEvaluation> getAnswerEvaluationsByAnswerIds(List<String> answerIds);
    
    long countQuestionEvaluations();
    
    long countAnswerEvaluations();
    
    String chat(String message);
    
    String analyzeComments(List<String> comments);
}