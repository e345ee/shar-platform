package com.course.entity;

/**
 * Supported question types.
 */
public enum TestQuestionType {
    /** One correct option out of 4. */
    SINGLE_CHOICE,
    /** Student enters a short text answer; system auto-checks it. */
    TEXT,
    /** Student enters an open-ended answer; teacher grades it manually. */
    OPEN
}
