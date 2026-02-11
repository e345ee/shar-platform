package com.course.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "methodist_teachers",
        uniqueConstraints = @UniqueConstraint(name = "uq_methodist_teacher", columnNames = {"methodist_id", "teacher_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodistTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "methodist_id", nullable = false)
    private User methodist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

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

    public User getMethodist() {
        return this.methodist;
    }

    public void setMethodist(User methodist) {
        this.methodist = methodist;
    }

    public User getTeacher() {
        return this.teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
