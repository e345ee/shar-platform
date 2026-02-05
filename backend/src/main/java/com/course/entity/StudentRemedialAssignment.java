package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Explicit assignment of a REMEDIAL_TASK activity to a concrete student.
 *
 * Rationale:
 * - REMEDIAL_TASK activities exist in a course (like weekly activities), but should not be visible
 *   to everyone.
 * - When a student has low results on a topic, the system assigns a matching remedial activity.
 */
@Entity
@Table(
        name = "student_remedial_assignments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_student_remedial_assignment", columnNames = {"student_id", "test_id"})
        },
        indexes = {
                @Index(name = "idx_sra_student", columnList = "student_id"),
                @Index(name = "idx_sra_course", columnList = "course_id"),
                @Index(name = "idx_sra_topic", columnList = "topic"),
                @Index(name = "idx_sra_student_course_week", columnList = "student_id,course_id,assigned_week_start")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRemedialAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Denormalized topic of the assigned remedial activity (copied from Test.topic).
     */
    @Column(name = "topic", length = 127, nullable = false)
    private String topic;

    /**
     * If the remedial activity is assigned to a week (like WEEKLY_STAR), store the same week start.
     */
    @Column(name = "assigned_week_start")
    private LocalDate assignedWeekStart;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
