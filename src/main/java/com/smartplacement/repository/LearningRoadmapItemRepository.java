package com.smartplacement.repository;

import com.smartplacement.entity.LearningRoadmapItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningRoadmapItemRepository extends JpaRepository<LearningRoadmapItem, Long> {
    List<LearningRoadmapItem> findByRoadmapIdOrderByWeekNumberAsc(Long roadmapId);
}