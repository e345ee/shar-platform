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

    /**
     * Selected option for SINGLE_CHOICE questions (1..4). Null for TEXT questions.
     */
    @Column(name = "selected_option")
    private Integer selectedOption;

    /**
     * Student's answer for TEXT / OPEN questions. Null for SINGLE_CHOICE questions.
     */
    @Column(name = "text_answer", length = 4096)
    private String textAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    /**
     * Points awarded for this answer (0..question.points). Stored to keep grading stable
     * even if methodist changes question points later.
     */
    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded = 0;

    /**
     * Teacher feedback for OPEN questions (optional).
     */
    @Column(name = "feedback", length = 2048)
    private String feedback;

    /**
     * When the teacher graded this answer (OPEN questions). Null means not graded yet.
     */
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

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
        if (pointsAwarded == null) {
            pointsAwarded = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
