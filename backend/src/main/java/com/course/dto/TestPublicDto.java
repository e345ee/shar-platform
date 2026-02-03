package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Same as TestDto but without publication status / correct answers.
 */
@Data
public class TestPublicDto {
    private Integer id;
    private Integer lessonId;
    private Integer courseId;

    private String title;
    private String description;
    private String topic;
    private LocalDateTime deadline;
    private LocalDateTime publishedAt;

    private Integer questionCount;
    private List<TestQuestionPublicDto> questions;
}
