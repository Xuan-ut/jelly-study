package com.jellystudy.repository;

import com.jellystudy.entity.QuestionEvaluation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuestionEvaluationRepository extends MongoRepository<QuestionEvaluation, String> {
    QuestionEvaluation findByQuestionId(String questionId);
    
    List<QuestionEvaluation> findByQuestionIdIn(List<String> questionIds);
}