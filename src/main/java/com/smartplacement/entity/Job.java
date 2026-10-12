package com.smartplacement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 150)
    private String location;

    @Column(name = "job_type", length = 50)
    @Builder.Default
    private String jobType = "FULL_TIME";

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "min_cgpa")
    @Builder.Default
    private Double minCgpa = 0.0;

    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 0;

    @Column(length = 30)
    @Builder.Default
    private String status = "OPEN"; // OPEN, CLOSED, DRAFT

    private LocalDate deadline;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
