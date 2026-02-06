package com.course.entity;

/**
 * Notification types supported by the system.
 */
public enum NotificationType {
    /** Teacher/methodist: a new request to join a class. */
    CLASS_JOIN_REQUEST,

    /** Teacher/methodist: student submitted an activity that requires manual grading (OPEN). */
    MANUAL_GRADING_REQUIRED,

    /** Student: a grade is available for an activity (attempt fully graded). */
    GRADE_RECEIVED,

    /** Student: an open-ended answer was checked and feedback is available. */
    OPEN_ANSWER_CHECKED,

    /** Student: a new weekly assignment is available for the current week. */
    WEEKLY_ASSIGNMENT_AVAILABLE,

    /** Student: a new achievement was awarded. */
    ACHIEVEMENT_AWARDED
}
