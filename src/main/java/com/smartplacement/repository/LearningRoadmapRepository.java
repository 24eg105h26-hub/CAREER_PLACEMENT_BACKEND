package com.smartplacement.repository;

import com.smartplacement.entity.LearningRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningRoadmapRepository extends JpaRepository<LearningRoadmap, Long> {
    List<LearningRoadmap> findByStudentId(Long studentId);
    Optional<LearningRoadmap> findFirstByStudentIdOrderByCreatedAtDesc(Long studentId);
}