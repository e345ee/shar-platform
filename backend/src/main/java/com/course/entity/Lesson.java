package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lessons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_lesson_title_in_course", columnNames = {"course_id", "title"}),
                @UniqueConstraint(name = "uq_lesson_order_in_course", columnNames = {"course_id", "order_index"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", length = 127, nullable = false)
    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @Column(name = "description", length = 2048)
    @Size(max = 2048)
    private String description;

    @Column(name = "presentation_url", length = 512)
    @Size(max = 512)
    private String presentationUrl;

    /**
     * 1-based ordering of lessons inside a course.
     */
    @Column(name = "order_index", nullable = false)
    @Min(1)
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

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
