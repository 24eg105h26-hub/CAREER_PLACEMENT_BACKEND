package com.smartplacement.repository;

import com.smartplacement.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(String status);

    List<Job> findByRecruiterId(Long recruiterId);

    List<Job> findByCompanyId(Long companyId);

    @Query("SELECT j FROM Job j WHERE j.status = 'OPEN' AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:jobType IS NULL OR j.jobType = :jobType) AND " +
           "(:minCgpa IS NULL OR j.minCgpa <= :minCgpa)")
    List<Job> filterJobs(@Param("location") String location,
                         @Param("jobType") String jobType,
                         @Param("minCgpa") Double minCgpa);
}
