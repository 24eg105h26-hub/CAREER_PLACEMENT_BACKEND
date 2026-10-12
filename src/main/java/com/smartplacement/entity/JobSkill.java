package com.smartplacement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "skill_type", length = 30)
    @Builder.Default
    private String skillType = "REQUIRED"; // REQUIRED, PREFERRED

    @Column(nullable = false)
    @Builder.Default
    private Integer weight = 3; // 3 for REQUIRED, 1 for PREFERRED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
