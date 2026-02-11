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

    
    @Column(name = "correct_option")
    @Min(1)
    @Max(4)
    private Integer correctOption;

    
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

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Test getTest() {
        return this.test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public Integer getOrderIndex() {
        return this.orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getQuestionText() {
        return this.questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOption1() {
        return this.option1;
    }

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public String getOption2() {
        return this.option2;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public String getOption3() {
        return this.option3;
    }

    public void setOption3(String option3) {
        this.option3 = option3;
    }

    public String getOption4() {
        return this.option4;
    }

    public void setOption4(String option4) {
        this.option4 = option4;
    }

    public Integer getCorrectOption() {
        return this.correctOption;
    }

    public void setCorrectOption(Integer correctOption) {
        this.correctOption = correctOption;
    }

    public String getCorrectTextAnswer() {
        return this.correctTextAnswer;
    }

    public void setCorrectTextAnswer(String correctTextAnswer) {
        this.correctTextAnswer = correctTextAnswer;
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
