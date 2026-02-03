package com.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestAttemptPublicDto {
    private Integer id;
    private Integer testId;
    private Integer lessonId;
    private Integer courseId;
    private Integer attemptNo;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Boolean isLate;
    private Integer score;
    private Integer maxScore;
    private List<TestAttemptAnswerPublicDto> answers;
}
