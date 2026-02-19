const API_BASE_URL = process.env.REACT_APP_API_URL || "";
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

