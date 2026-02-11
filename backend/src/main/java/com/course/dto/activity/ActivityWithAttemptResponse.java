package com.course.dto.activity;

import lombok.Data;

@Data
public class ActivityWithAttemptResponse {
    private com.course.dto.activity.ActivityResponse activity;
    private com.course.dto.attempt.AttemptStatusResponse latestAttempt;

    

    public com.course.dto.activity.ActivityResponse getActivity() {
        return this.activity;
    }

    public void setActivity(com.course.dto.activity.ActivityResponse activity) {
        this.activity = activity;
    }

    public com.course.dto.attempt.AttemptStatusResponse getLatestAttempt() {
        return this.latestAttempt;
    }

    public void setLatestAttempt(com.course.dto.attempt.AttemptStatusResponse latestAttempt) {
        this.latestAttempt = latestAttempt;
    }

}
