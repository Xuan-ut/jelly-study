package com.jellystudy.repository.mongo;

import com.jellystudy.entity.StudyEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyEventRepository extends MongoRepository<StudyEvent, String> {
    List<StudyEvent> findByUserId(Long userId);
    List<StudyEvent> findByEventType(String eventType);
    List<StudyEvent> findByUserIdOrderByCreateTimeDesc(Long userId);
}
