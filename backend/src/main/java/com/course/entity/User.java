package com.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
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
    @Size(min = 3, max = 127, message = "Email must be between 3 and 127 characters")
    private String email;

    @Column(name = "password", length = 127, nullable = false)
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 127, message = "Password must be between 6 and 127 characters")
    @ToString.Exclude
    private String password;

    @Column(name = "bio", columnDefinition = "TEXT")
    @Pattern(regexp = "^(?!\\s*$).+", message = "Bio must not be blank")
    @Size(max = 2048, message = "Bio must be at most 2048 characters")
    private String bio;

    @Column(name = "photo", columnDefinition = "TEXT")
    @Pattern(regexp = "^(?!\\s*$).+", message = "Photo must not be blank")
    @Size(max = 1024, message = "Photo must be at most 1024 characters")
    private String photo;

    @Column(name = "tg_id", length = 127, unique = true)
    @Pattern(regexp = "^(?!\\s*$).+", message = "Telegram ID must not be blank")
    @Size(max = 127, message = "Telegram ID must be at most 127 characters")
    private String tgId;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    

    public void setName(String name) {
        this.name = validateRequiredString(name, "Name", 1, 63);
    }

    public void setEmail(String email) {
        String v = validateRequiredString(email, "Email", 3, 127);
        
        if (!v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email must be a valid email address");
        }
        this.email = v;
    }

    public void setPassword(String password) {
        this.password = validateRequiredString(password, "Password", 6, 127);
    }

    public void setBio(String bio) {
        if (bio == null) {
            this.bio = null;
            return;
        }
        this.bio = validateOptionalString(bio, "Bio", 2048);
    }

    public void setPhoto(String photo) {
        if (photo == null) {
            this.photo = null;
            return;
        }
        this.photo = validateOptionalString(photo, "Photo", 1024);
    }

    public void setTgId(String tgId) {
        if (tgId == null || tgId.isBlank()) {
            this.tgId = null;
            return;
        }
        this.tgId = validateRequiredString(tgId, "Telegram ID", 1, 127);
    }

    private static String validateRequiredString(String value, String field, int min, int max) {
        if (value == null) {
            throw new IllegalArgumentException(field + " cannot be null");
        }
        String v = value.trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        if (v.length() < min || v.length() > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max + " characters");
        }
        return v;
    }

    private static String validateOptionalString(String value, String field, int max) {
        String v = value.trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        if (v.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
        return v;
    }

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Role getRole() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public String getBio() {
        return this.bio;
    }

    public String getPhoto() {
        return this.photo;
    }

    public String getTgId() {
        return this.tgId;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

}
