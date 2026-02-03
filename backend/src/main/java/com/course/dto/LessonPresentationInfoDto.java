package com.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonPresentationInfoDto {
    private boolean hasPresentation;
    private int pageCount;
}
