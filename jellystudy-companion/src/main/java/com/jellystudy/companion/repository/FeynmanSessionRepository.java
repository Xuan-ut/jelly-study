package com.jellystudy.companion.repository;

import com.jellystudy.companion.entity.FeynmanSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeynmanSessionRepository extends MongoRepository<FeynmanSession, String> {

    Optional<FeynmanSession> findBySessionId(String sessionId);

    List<FeynmanSession> findByUserIdAndKnowledgeId(Long userId, String knowledgeId);

    List<FeynmanSession> findByUserId(Long userId);
}
