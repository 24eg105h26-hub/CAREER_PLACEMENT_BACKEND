package com.smartplacement.repository;

import com.smartplacement.entity.MentorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorAssignmentRepository extends JpaRepository<MentorAssignment, Long> {

    List<MentorAssignment> findByMentorId(Long mentorId);
    boolean existsByMentorIdAndStudentId(Long mentorId, Long studentId);
}
