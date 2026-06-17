package com.jellystudy.repository;

import com.jellystudy.entity.AnswerEvaluation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AnswerEvaluationRepository extends MongoRepository<AnswerEvaluation, String> {
    AnswerEvaluation findByAnswerId(String answerId);
    
    List<AnswerEvaluation> findByAnswerIdIn(List<String> answerIds);
    
    List<AnswerEvaluation> findByQuestionId(String questionId);
}