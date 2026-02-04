package com.course.entity;

/**
 * Canonical set of application roles.
 *
 * Stored in DB as Postgres enum (role_name) and mapped via Hibernate NAMED_ENUM.
 */
public enum RoleName {
    ADMIN,
    METHODIST,
    TEACHER,
    STUDENT;

    /**
     * Spring Security authority string (with ROLE_ prefix).
     */
    public String authority() {
        return "ROLE_" + name();
    }
}
