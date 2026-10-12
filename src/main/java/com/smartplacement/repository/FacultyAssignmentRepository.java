package com.smartplacement.repository;

import com.smartplacement.entity.FacultyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultyAssignmentRepository extends JpaRepository<FacultyAssignment, Long> {

    List<FacultyAssignment> findByFacultyId(Long facultyId);
    boolean existsByFacultyIdAndStudentId(Long facultyId, Long studentId);
}
