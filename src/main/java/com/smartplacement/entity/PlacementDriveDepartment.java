package com.smartplacement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "placement_drive_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementDriveDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drive_id", nullable = false)
    private PlacementDrive drive;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
