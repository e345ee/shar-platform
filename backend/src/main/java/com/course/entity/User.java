package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    @NotNull(message = "Role cannot be null")
    private Role role;

    @Column(name = "name", length = 63, nullable = false, unique = true)
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 1, max = 63, message = "Name must be between 1 and 63 characters")
    private String name;

    @Column(name = "email", length = 127, nullable = false, unique = true)
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(min = 1, max = 127, message = "Email must be between 1 and 127 characters")
    private String email;

    @Column(name = "password", length = 127, nullable = false)
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 1, max = 127, message = "Password must be between 1 and 127 characters")
    @ToString.Exclude
    private String password;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "photo", columnDefinition = "TEXT")
    private String photo;

    @Column(name = "tg_id", length = 127, unique = true)
    private String tgId;
}
