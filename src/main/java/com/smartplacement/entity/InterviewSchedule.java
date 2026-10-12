package com.smartplacement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "interview_date", nullable = false)
    private LocalDateTime interviewDate;

    @Column(length = 50)
    @Builder.Default
    private String mode = "ONLINE"; // ONLINE, IN_PERSON

    @Column(name = "link_or_location")
    private String linkOrLocation;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 30)
    @Builder.Default
    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, CANCELLED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
