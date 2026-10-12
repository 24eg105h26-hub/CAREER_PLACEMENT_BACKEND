package com.smartplacement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "parsed_data_json", columnDefinition = "JSON")
    private String parsedDataJson;

    @Column(name = "resume_score")
    @Builder.Default
    private Double resumeScore = 0.0;

    @Column(name = "ats_score")
    @Builder.Default
    private Double atsScore = 0.0;

    @Column(name = "tech_score")
    @Builder.Default
    private Double techScore = 0.0;

    @Column(name = "project_score")
    @Builder.Default
    private Double projectScore = 0.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
