package com.jellystudy.companion.repository;

import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeTreeSnapshotRepository extends MongoRepository<KnowledgeTreeSnapshot, String> {

    Optional<KnowledgeTreeSnapshot> findByUserIdAndDate(Long userId, LocalDate date);

    List<KnowledgeTreeSnapshot> findByUserIdOrderByDateDesc(Long userId);

    Optional<KnowledgeTreeSnapshot> findTopByUserIdOrderByDateDesc(Long userId);
}
