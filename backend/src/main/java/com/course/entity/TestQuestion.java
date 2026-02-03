package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "test_questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_test_question_order", columnNames = {"test_id", "order_index"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    /** 1-based ordering of questions inside a test. */
    @Column(name = "order_index", nullable = false)
    @Min(1)
    private Integer orderIndex;

    @Column(name = "question_text", length = 2048, nullable = false)
    @NotBlank
    @Size(min = 1, max = 2048)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 32, nullable = false)
    private TestQuestionType questionType = TestQuestionType.SINGLE_CHOICE;

    /**
     * How many points the question is worth.
     */
    @Column(name = "points", nullable = false)
    @NotNull
    @Min(1)
    private Integer points = 1;

    @Column(name = "option_1", length = 512)
    @Size(max = 512)
    private String option1;

    @Column(name = "option_2", length = 512)
    @Size(max = 512)
    private String option2;

    @Column(name = "option_3", length = 512)
    @Size(max = 512)
    private String option3;

    @Column(name = "option_4", length = 512)
    @Size(max = 512)
    private String option4;

    /**
     * 1..4 (index of the correct option).
     */
    @Column(name = "correct_option")
    @Min(1)
    @Max(4)
    private Integer correctOption;

    /**
     * Correct answer for TEXT questions.
     * Stored trimmed; matching is done with trim + case-insensitive compare.
     */
    @Column(name = "correct_text_answer", length = 512)
    @Size(max = 512)
    private String correctTextAnswer;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
