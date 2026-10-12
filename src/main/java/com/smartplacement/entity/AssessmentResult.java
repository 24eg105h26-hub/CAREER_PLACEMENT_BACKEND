package com.smartplacement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(nullable = false)
    private Double percentage;

    @Column(length = 30)
    @Builder.Default
    private String status = "COMPLETED";

    @CreationTimestamp
    @Column(name = "completed_at", updatable = false)
    private LocalDateTime completedAt;
}