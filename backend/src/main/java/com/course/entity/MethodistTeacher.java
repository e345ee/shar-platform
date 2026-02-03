package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Explicit ownership link between METHODIST and TEACHER.
 *
 * Needed to enforce: a methodist can manage (edit/delete/assign) only their own teachers.
 */
@Entity
@Table(
        name = "methodist_teachers",
        uniqueConstraints = @UniqueConstraint(name = "uq_methodist_teacher", columnNames = {"methodist_id", "teacher_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodistTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "methodist_id", nullable = false)
    private User methodist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
