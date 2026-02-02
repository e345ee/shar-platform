package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_join_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_join_request_class_email",
                columnNames = {"class_id", "email"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private StudyClass studyClass;

    @Column(name = "name", length = 63, nullable = false)
    @NotBlank
    @Size(min = 1, max = 63)
    private String name;

    @Column(name = "email", length = 127, nullable = false)
    @NotBlank
    @Email
    @Size(min = 1, max = 127)
    private String email;

    @Column(name = "tg_id", length = 127)
    private String tgId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
