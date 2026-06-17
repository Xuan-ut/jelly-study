package com.jellystudy.repository.mongo;

import com.jellystudy.entity.DailyTaskBoard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyTaskBoardRepository extends MongoRepository<DailyTaskBoard, String> {
    Optional<DailyTaskBoard> findByUserIdAndDate(Long userId, String date);
    List<DailyTaskBoard> findByUserIdOrderByDateDesc(Long userId);
    List<DailyTaskBoard> findByUserIdAndDateBetween(Long userId, String startDate, String endDate);
}
