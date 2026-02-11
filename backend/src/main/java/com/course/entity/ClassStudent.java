package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_students",
        uniqueConstraints = @UniqueConstraint(name = "uq_class_student", columnNames = {"class_id", "student_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private StudyClass studyClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    
    @Column(name = "course_closed_at")
    private LocalDateTime courseClosedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCourseClosedAt() {
        return this.courseClosedAt;
    }

    public void setCourseClosedAt(LocalDateTime courseClosedAt) {
        this.courseClosedAt = courseClosedAt;
    }

}
