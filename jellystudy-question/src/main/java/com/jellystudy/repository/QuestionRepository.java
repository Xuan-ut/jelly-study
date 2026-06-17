package com.jellystudy.repository;

import com.jellystudy.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends MongoRepository<Question, String> {
    List<Question> findByKnowledgePointIdsContaining(String knowledgePointId);
    List<Question> findTop10ByOrderByLikeCountDesc();
    List<Question> findTop10ByOrderByAnswerCountDesc();
    List<Question> findByTitleContaining(String keyword);
    List<Question> findByContentContaining(String keyword);
    Page<Question> findAll(Pageable pageable);
}