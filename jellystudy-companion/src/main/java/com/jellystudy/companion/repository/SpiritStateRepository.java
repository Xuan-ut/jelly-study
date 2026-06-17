package com.jellystudy.companion.repository;

import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.enums.SpiritEmotion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpiritStateRepository extends MongoRepository<SpiritState, String> {

    Optional<SpiritState> findByUserId(Long userId);

    List<SpiritState> findByEmotion(SpiritEmotion emotion);

    List<SpiritState> findByLevel(Integer level);
}
