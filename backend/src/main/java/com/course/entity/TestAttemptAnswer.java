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

    
    @Column(name = "selected_option")
    private Integer selectedOption;

    
    @Column(name = "text_answer", length = 4096)
    private String textAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    
    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded = 0;

    
    @Column(name = "feedback", length = 2048)
    private String feedback;

    
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

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TestAttempt getAttempt() {
        return this.attempt;
    }

    public void setAttempt(TestAttempt attempt) {
        this.attempt = attempt;
    }

    public TestQuestion getQuestion() {
        return this.question;
    }

    public void setQuestion(TestQuestion question) {
        this.question = question;
    }

    public Integer getSelectedOption() {
        return this.selectedOption;
    }

    public void setSelectedOption(Integer selectedOption) {
        this.selectedOption = selectedOption;
    }

    public String getTextAnswer() {
        return this.textAnswer;
    }

    public void setTextAnswer(String textAnswer) {
        this.textAnswer = textAnswer;
    }

    public String getFeedback() {
        return this.feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getGradedAt() {
        return this.gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
