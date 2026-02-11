package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "class_achievement_feed")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassAchievementFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "class_id", nullable = false)
    private StudyClass studyClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "awarded_by", nullable = true)
    private User awardedBy;

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (awardedAt == null) awardedAt = now;
    }

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public StudyClass getStudyClass() {
        return this.studyClass;
    }

    public void setStudyClass(StudyClass studyClass) {
        this.studyClass = studyClass;
    }

    public User getStudent() {
        return this.student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Achievement getAchievement() {
        return this.achievement;
    }

    public void setAchievement(Achievement achievement) {
        this.achievement = achievement;
    }

    public User getAwardedBy() {
        return this.awardedBy;
    }

    public void setAwardedBy(User awardedBy) {
        this.awardedBy = awardedBy;
    }

    public LocalDateTime getAwardedAt() {
        return this.awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
