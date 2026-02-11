package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "achievements",
        uniqueConstraints = @UniqueConstraint(name = "uq_achievement_title_in_course", columnNames = {"course_id", "title"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", length = 127, nullable = false)
    @NotBlank(message = "title is required")
    @Size(min = 1, max = 127, message = "title must be between 1 and 127 characters")
    private String title;

    @Column(name = "joke_description", length = 1024, nullable = false)
    @NotBlank(message = "jokeDescription is required")
    @Size(min = 1, max = 1024, message = "jokeDescription must be between 1 and 1024 characters")
    private String jokeDescription;

    @Column(name = "description", length = 2048, nullable = false)
    @NotBlank(message = "description is required")
    @Size(min = 1, max = 2048, message = "description must be between 1 and 2048 characters")
    private String description;

    @Column(name = "photo_url", length = 512, nullable = false)
    @NotBlank(message = "photoUrl is required")
    @Size(min = 1, max = 512, message = "photoUrl must be between 1 and 512 characters")
    private String photoUrl;

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

    public String getJokeDescription() {
        return this.jokeDescription;
    }

    public void setJokeDescription(String jokeDescription) {
        this.jokeDescription = jokeDescription;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoUrl() {
        return this.photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
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
