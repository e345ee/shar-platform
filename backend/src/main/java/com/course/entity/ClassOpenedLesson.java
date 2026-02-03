package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_opened_lessons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_class_opened_lesson", columnNames = {"class_id", "lesson_id"})
        },
        indexes = {
                @Index(name = "idx_class_opened_lessons_class", columnList = "class_id"),
                @Index(name = "idx_class_opened_lessons_lesson", columnList = "lesson_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassOpenedLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private StudyClass studyClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @PrePersist
    public void onCreate() {
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
    }
}
