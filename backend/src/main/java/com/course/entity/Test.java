package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Lesson attachment (optional): for HOMEWORK_TEST and CONTROL_WORK.
     * WEEKLY_STAR is not attached to a lesson.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    /**
     * Course owner of the activity (always present).
     * For lesson-attached activities it is derived from the lesson's course.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", length = 32, nullable = false)
    private ActivityType activityType = ActivityType.HOMEWORK_TEST;

    /**
     * Weight multiplier applied to score/maxScore when calculating weighted results.
     * For CONTROL_WORK and WEEKLY_STAR it is typically > 1.
     */
    @Column(name = "weight_multiplier", nullable = false)
    private Integer weightMultiplier = 1;

    /**
     * For WEEKLY_STAR: week start date (Monday) when this activity is assigned/visible.
     * Null means not assigned yet.
     */
    @Column(name = "assigned_week_start")
    private java.time.LocalDate assignedWeekStart;
    /**
     * Time limit in seconds for CONTROL_WORK after a student starts an attempt.
     * Null means no per-attempt time limit.
     */
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "title", length = 127, nullable = false)
    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @Column(name = "description", length = 2048)
    @Size(max = 2048)
    private String description;

    @Column(name = "topic", length = 127, nullable = false)
    @NotBlank
    @Size(min = 1, max = 127)
    private String topic;

    @Column(name = "deadline", nullable = false)
    @NotNull
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private TestStatus status = TestStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<TestQuestion> questions = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = TestStatus.DRAFT;
        }
        if (activityType == null) {
            activityType = ActivityType.HOMEWORK_TEST;
        }
        if (weightMultiplier == null || weightMultiplier < 1) {
            weightMultiplier = 1;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
