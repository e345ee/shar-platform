package com.course.entity;


public enum RoleName {
    ADMIN,
    METHODIST,
    TEACHER,
    STUDENT;

    
    public String authority() {
        return "ROLE_" + name();
    }
}
