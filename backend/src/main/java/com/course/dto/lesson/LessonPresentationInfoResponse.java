package com.course.dto.lesson;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonPresentationInfoResponse {
    private boolean hasPresentation;
    private int pageCount;

    

    public boolean isHasPresentation() {
        return this.hasPresentation;
    }

    public void setHasPresentation(boolean hasPresentation) {
        this.hasPresentation = hasPresentation;
    }

    public int getPageCount() {
        return this.pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

}
