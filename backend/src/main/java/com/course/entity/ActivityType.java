package com.course.entity;

/**
 * Activity types in a course.
 * - HOMEWORK_TEST: regular homework test attached to a lesson
 * - CONTROL_WORK: like a test but with higher weight
 * - WEEKLY_STAR: weekly "star" assignment, not attached to a specific lesson; assigned to a week.
 */
public enum ActivityType {
    HOMEWORK_TEST,
    CONTROL_WORK,
    WEEKLY_STAR
}
