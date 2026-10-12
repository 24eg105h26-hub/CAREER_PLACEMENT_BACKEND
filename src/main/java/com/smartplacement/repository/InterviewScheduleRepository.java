package com.smartplacement.repository;

import com.smartplacement.entity.InterviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {

    List<InterviewSchedule> findByApplicationId(Long applicationId);

    List<InterviewSchedule> findByApplicationStudentId(Long studentId);
}
