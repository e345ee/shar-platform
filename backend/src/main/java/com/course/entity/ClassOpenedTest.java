package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "class_opened_tests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_class_opened_test", columnNames = {"class_id", "test_id"})
        },
        indexes = {
                @Index(name = "idx_class_opened_tests_class", columnList = "class_id"),
                @Index(name = "idx_class_opened_tests_test", columnList = "test_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassOpenedTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private StudyClass studyClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @PrePersist
    public void onCreate() {
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
    }
}
