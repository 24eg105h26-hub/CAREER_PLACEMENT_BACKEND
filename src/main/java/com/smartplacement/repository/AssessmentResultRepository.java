package com.smartplacement.repository;

import com.smartplacement.entity.AssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long> {
    List<AssessmentResult> findByStudentIdOrderByCompletedAtDesc(Long studentId);
    List<AssessmentResult> findByAssessmentIdOrderByCompletedAtDesc(Long assessmentId);
}