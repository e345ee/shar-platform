package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "test_attempt_answers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_attempt_question",
                        columnNames = {"attempt_id", "question_id"}
                )
        },
        indexes = {
                @Index(name = "idx_attempt_answer_attempt", columnList = "attempt_id"),
                @Index(name = "idx_attempt_answer_question", columnList = "question_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private TestQuestion question;

    @Column(name = "selected_option", nullable = false)
    private Integer selectedOption;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (isCorrect == null) {
            isCorrect = false;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
