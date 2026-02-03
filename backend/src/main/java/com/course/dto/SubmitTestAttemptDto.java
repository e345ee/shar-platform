package com.course.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitTestAttemptDto {
    private List<SubmitTestAttemptAnswerDto> answers;
}
