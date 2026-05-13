package com.edulearn.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    private String level; // Beginner, Intermediate, Advanced

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer instructorId; // Stores userId of the instructor

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false)
    private Integer totalDuration; // in minutes

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isPublished = false;

    @Column(nullable = false)
    private LocalDate createdAt = LocalDate.now();

    @Column(length = 50)
    private String language;
    

    @Size(max = 500)
    @Column(length = 500)
    private String preview;  // short preview text or video URL

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private Integer discount = 0;  // percentage (0–100)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CourseStatus status = CourseStatus.DRAFT;
}
