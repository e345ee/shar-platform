package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "title is required")
    @Size(min = 1, max = 127, message = "title must be between 1 and 127 characters")
    private String title;

    @Column(name = "description", length = 2048)
    @Pattern(regexp = "^(?!\\s*$).+", message = "description must not be blank")
    @Size(max = 2048, message = "description must be at most 2048 characters")
    private String description;

    @Column(name = "presentation_url", length = 512)
    @Pattern(regexp = "^(?!\\s*$).+", message = "presentationUrl must not be blank")
    @Size(max = 512, message = "presentationUrl must be at most 512 characters")
    private String presentationUrl;

    
    @Column(name = "order_index", nullable = false)
    @NotNull(message = "orderIndex is required")
    @Min(value = 1, message = "orderIndex must be >= 1")
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull(message = "course is required")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull(message = "createdBy is required")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull
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

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPresentationUrl() {
        return this.presentationUrl;
    }

    public void setPresentationUrl(String presentationUrl) {
        this.presentationUrl = presentationUrl;
    }

    public Integer getOrderIndex() {
        return this.orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Course getCourse() {
        return this.course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public User getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
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
