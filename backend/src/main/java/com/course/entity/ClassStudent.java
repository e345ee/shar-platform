package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_students",
        uniqueConstraints = @UniqueConstraint(name = "uq_class_student", columnNames = {"class_id", "student_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private StudyClass studyClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When set, the course is considered closed/completed for the student (for this class/course).
     * Teacher/Methodist marks completion explicitly.
     */
    @Column(name = "course_closed_at")
    private LocalDateTime courseClosedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
