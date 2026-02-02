package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @Column(name = "joke_description", length = 1024, nullable = false)
    @NotBlank
    @Size(min = 1, max = 1024)
    private String jokeDescription;

    @Column(name = "description", length = 2048, nullable = false)
    @NotBlank
    @Size(min = 1, max = 2048)
    private String description;

    @Column(name = "photo_url", length = 512, nullable = false)
    @NotBlank
    @Size(min = 1, max = 512)
    private String photoUrl;

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
