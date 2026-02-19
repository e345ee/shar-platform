const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";
const AUTH_TOKEN_KEY = "auth_access_token";

function getAccessToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY) || "";
}

async function requestJson(path, { method = "GET", body } = {}) {
    const token = getAccessToken();
    const headers = {
        Accept: "application/json",
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }
    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (response.status === 204) {
        return null;
    }

    const text = await response.text();
    let payload = null;
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch (e) {
            payload = null;
        }
    }

    if (!response.ok) {
        const error = new Error(payload?.message || "Request failed");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return payload;
}

export async function listMyCourses() {
    const data = await requestJson("/api/me/courses");
    return Array.isArray(data) ? data : [];
}

export async function getMyCoursePage(courseId) {
    return requestJson(`/api/me/courses/${courseId}/page`);
}

export async function getActivityById(activityId) {
    return requestJson(`/api/activities/${activityId}`);
}

export async function startAttempt(activityId) {
    return requestJson(`/api/activities/${activityId}/attempts`, {
        method: "POST",
    });
}

export async function submitAttempt(attemptId, answers) {
    return requestJson(`/api/attempts/${attemptId}/submit`, {
        method: "POST",
        body: { answers },
    });
}

export async function getAttempt(attemptId) {
    return requestJson(`/api/attempts/${attemptId}`);
}

export async function sendCompletionEmail(courseId) {
    return requestJson(`/api/me/courses/${courseId}/completion-email`, {
        method: "POST",
    });
}

export async function getMyAchievementsPage() {
    return requestJson("/api/me/achievements/page");
}

export async function getMyStatisticsOverview() {
    return requestJson("/api/me/statistics/overview");
}

export async function getMyStatisticsTopics(courseId) {
    const query = courseId ? `?courseId=${encodeURIComponent(courseId)}` : "";
    return requestJson(`/api/me/statistics/topics${query}`);
}

export async function getClassAchievementFeed(classId, page = 0, size = 20) {
    return requestJson(`/api/classes/${classId}/achievement-feed?page=${page}&size=${size}`);
}

export async function createJoinRequest(classCode) {
    return requestJson("/api/join-requests", {
        method: "POST",
        body: { classCode },
    });
}

export async function listMyLessonsInCourse(courseId) {
    const data = await requestJson(`/api/me/courses/${courseId}/lessons`);
    return Array.isArray(data) ? data : [];
}

export async function listActivitiesByLesson(lessonId) {
    const data = await requestJson(`/api/lessons/${lessonId}/activities`);
    return Array.isArray(data) ? data : [];
}

export async function listWeeklyActivitiesByCourse(courseId) {
    const data = await requestJson(`/api/courses/${courseId}/activities/weekly`);
    return Array.isArray(data) ? data : [];
}

export async function listMyAttempts(activityId) {
    const data = await requestJson(`/api/activities/${activityId}/attempts/my`);
    return Array.isArray(data) ? data : [];
}

export async function getLatestCompletedAttempt(activityId) {
    return requestJson(`/api/me/activities/${activityId}/attempts/latest`);
}

export async function getLatestCompletedAttemptByLesson(lessonId) {
    return requestJson(`/api/me/lessons/${lessonId}/activity/attempts/latest`);
}

// Profile functions
export async function getMyProfile() {
    return requestJson("/api/me");
}

export async function updateMyProfile(dto) {
    return requestJson("/api/me/profile", {
        method: "PATCH",
        body: dto,
    });
}

export async function uploadMyAvatar(file) {
    const token = getAccessToken();
    const formData = new FormData();
    formData.append("file", file);

    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const response = await fetch(`${API_BASE_URL}/api/me/avatar`, {
        method: "POST",
        headers,
        body: formData,
    });

    if (!response.ok) {
        const text = await response.text();
        let payload = null;
        if (text) {
            try {
                payload = JSON.parse(text);
            } catch (e) {
                payload = null;
            }
        }
        const error = new Error(payload?.message || "Не удалось загрузить фото");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return response.json();
}

export async function deleteMyAvatar() {
    return requestJson("/api/me/avatar", {
        method: "DELETE",
    });
}
