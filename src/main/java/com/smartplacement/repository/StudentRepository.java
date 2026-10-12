package com.smartplacement.repository;

import com.smartplacement.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    List<Student> findByDepartmentId(Long departmentId);

    Optional<Student> findByRollNumber(String rollNumber);
}
