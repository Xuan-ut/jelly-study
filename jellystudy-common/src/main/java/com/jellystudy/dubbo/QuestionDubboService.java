package com.jellystudy.dubbo;

import com.jellystudy.entity.Answer;
import com.jellystudy.entity.Comment;
import com.jellystudy.entity.Question;
import org.springframework.data.domain.Page;

import java.util.List;

public interface QuestionDubboService {
    Question create(Question question);
    
    Question findById(String id);
    
    List<Question> findAll();
    
    Page<Question> findByPage(int page, int size);
    
    Question update(Question question);
    
    void delete(String id);
    
    Question addAnswer(String questionId, Answer answer);
    
    Question addComment(String questionId, String answerId, Comment comment);
    
    Question like(String questionId);
    
    Answer likeAnswer(String questionId, String answerId);
    
    Comment likeComment(String questionId, String answerId, String commentId);
    
    List<Question> getHotQuestions();
    
    List<Question> getRecommendedQuestions();
    
    List<Question> getQuestionsByKnowledgePoint(String knowledgePointId);
    
    List<Question> searchQuestions(String keyword);
    
    long count();
}