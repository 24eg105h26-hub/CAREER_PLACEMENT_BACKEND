package com.smartplacement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_roadmap_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningRoadmapItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private LearningRoadmap roadmap;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(nullable = false, length = 150)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "duration_weeks")
    @Builder.Default
    private Integer durationWeeks = 1;

    @Column(name = "resource_link")
    private String resourceLink;

    @Column(length = 30)
    @Builder.Default
    private String status = "NOT_STARTED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}