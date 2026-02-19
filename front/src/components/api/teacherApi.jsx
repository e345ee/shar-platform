const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";
const AUTH_TOKEN_KEY = "auth_access_token";

function getAccessToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY) || "";
}

async function requestJson(path, { method = "GET", body, params } = {}) {
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

    let url = `${API_BASE_URL}${path}`;
    if (params) {
        const searchParams = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                searchParams.append(key, String(value));
            }
        });
        url += `?${searchParams.toString()}`;
    }

    console.log("[requestJson] Making request", { method, url, hasToken: !!token, hasBody: body !== undefined });

    let response;
    try {
        response = await fetch(url, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined,
        });
        console.log("[requestJson] Response received", { method, url, status: response.status, statusText: response.statusText, ok: response.ok });
    } catch (networkError) {
        console.error("[requestJson] Network error", { method, url, error: networkError });
        const error = new Error("Network error: " + (networkError?.message || "Failed to fetch"));
        error.isNetworkError = true;
        error.originalError = networkError;
        throw error;
    }

    if (response.status === 204 || response.status === 200) {
        // Для 204 и 200 без тела возвращаем null
        const contentType = response.headers.get("content-type");
        if (!contentType || !contentType.includes("application/json")) {
            console.log("[requestJson] No JSON body, returning null", { status: response.status });
            return null;
        }
    }

    const text = await response.text();
    let payload = null;
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch (e) {
            console.warn("[requestJson] Failed to parse JSON", { text, error: e });
            payload = null;
        }
    }

    if (!response.ok) {
        console.error("[requestJson] Request failed", { method, url, status: response.status, payload });
        const error = new Error(payload?.message || `Request failed with status ${response.status}`);
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    console.log("[requestJson] Request successful", { method, url, status: response.status, payload });
    return payload;
}

function mapStudyClass(sc) {
    return {
        id: sc?.id,
        name: sc?.name || "",
        courseId: sc?.courseId ?? null,
        courseName: sc?.courseName || "",
        teacherId: sc?.teacherId ?? null,
        teacherName: sc?.teacherName || "",
        joinCode: sc?.joinCode || "",
    };
}

function mapUser(user) {
    return {
        id: user?.id,
        name: user?.name || "",
        email: user?.email || "",
        bio: user?.bio || "",
        photo: user?.photo || "",
        tgId: user?.tgId || "",
        courseClosedAt:
            user?.courseClosedAt ||
            user?.course_closed_at ||
            user?.closedCourseAt ||
            null,
    };
}

function mapJoinRequest(req) {
    return {
        id: req?.id,
        studentId: req?.studentId ?? null,
        studentName: req?.name || "", // Бэкенд возвращает 'name', а не 'studentName'
        studentEmail: req?.email || "", // Бэкенд возвращает 'email', а не 'studentEmail'
        classId: req?.classId ?? null,
        className: req?.className || "",
        createdAt: req?.createdAt || "",
        status: req?.status || "",
        tgId: req?.tgId || "",
    };
}

function mapAttempt(attempt) {
    return {
        id: attempt?.id,
        testId: attempt?.testId ?? null,
        activityId: attempt?.testId ?? null, // Для совместимости
        activityTitle: attempt?.activityTitle || "",
        activityType: attempt?.activityType || "",
        studentId: attempt?.studentId ?? null,
        studentName: attempt?.studentName || "",
        studentEmail: attempt?.studentEmail || "",
        classId: attempt?.classId ?? null,
        className: attempt?.className || "",
        courseId: attempt?.courseId ?? null,
        courseName: attempt?.courseName || "",
        status: attempt?.status || "",
        score: attempt?.score ?? null,
        maxScore: attempt?.maxScore ?? null,
        grade: attempt?.score ?? null, // Для совместимости
        maxGrade: attempt?.maxScore ?? null, // Для совместимости
        startedAt: attempt?.startedAt || "",
        submittedAt: attempt?.submittedAt || "",
        gradedAt: attempt?.gradedAt || "",
        answers: attempt?.answers || [],
        attemptNumber: attempt?.attemptNumber ?? null,
        percent: attempt?.percent ?? null,
    };
}

function mapPendingAttempt(attempt) {
    return {
        attemptId: attempt?.attemptId ?? null,
        id: attempt?.attemptId ?? null, // Для совместимости
        testId: attempt?.testId ?? null,
        activityId: attempt?.testId ?? null, // Для совместимости
        activityTitle: attempt?.activityTitle || "", // Будет загружено отдельно
        activityType: attempt?.activityType || "", // Будет загружено отдельно
        studentId: attempt?.studentId ?? null,
        studentName: attempt?.studentName || "",
        studentEmail: attempt?.studentEmail || "",
        classId: attempt?.classId ?? null,
        className: attempt?.className || "",
        courseId: attempt?.courseId ?? null,
        courseName: attempt?.courseName || "",
        submittedAt: attempt?.submittedAt || "",
        ungradedOpenCount: attempt?.ungradedOpenCount ?? 0,
    };
}

function mapLesson(lesson) {
    return {
        id: lesson?.id,
        courseId: lesson?.courseId ?? null,
        orderIndex: lesson?.orderIndex ?? null,
        title: lesson?.title || "",
        description: lesson?.description || "",
        presentationUrl: lesson?.presentationUrl || "",
        createdAt: lesson?.createdAt || "",
        updatedAt: lesson?.updatedAt || "",
    };
}

function mapActivity(activity) {
    return {
        id: activity?.id,
        lessonId: activity?.lessonId ?? null,
        courseId: activity?.courseId ?? null,
        activityType: activity?.activityType || "",
        timeLimitSeconds: activity?.timeLimitSeconds ?? null,
        title: activity?.title || "",
        description: activity?.description || "",
        topic: activity?.topic || "",
        deadline: activity?.deadline || "",
        status: activity?.status || "",
        questionCount: activity?.questionCount ?? 0,
        createdByName: activity?.createdByName || "",
    };
}

function mapAchievement(achievement) {
    return {
        id: achievement?.id ?? null,
        courseId: achievement?.courseId ?? null,
        title: achievement?.title || "",
        jokeDescription: achievement?.jokeDescription || "",
        description: achievement?.description || "",
        photoUrl: achievement?.photoUrl || "",
        createdById: achievement?.createdById ?? null,
        createdByName: achievement?.createdByName || "",
        createdAt: achievement?.createdAt || "",
        updatedAt: achievement?.updatedAt || "",
    };
}

function mapStudentAchievement(item) {
    return {
        id: item?.id ?? null,
        studentId: item?.studentId ?? null,
        studentName: item?.studentName || "",
        achievementId: item?.achievementId ?? null,
        achievementTitle: item?.achievementTitle || "",
        achievementPhotoUrl: item?.achievementPhotoUrl || "",
        achievementCourseId: item?.achievementCourseId ?? null,
        achievementJokeDescription: item?.achievementJokeDescription || "",
        achievementDescription: item?.achievementDescription || "",
        awardedById: item?.awardedById ?? null,
        awardedByName: item?.awardedByName || "",
        awardedAt: item?.awardedAt || "",
    };
}

function mapNotification(notification) {
    return {
        id: notification?.id ?? null,
        type: notification?.type || "",
        title: notification?.title || "",
        message: notification?.message || "",
        isRead: Boolean(notification?.read),
        courseId: notification?.courseId ?? null,
        classId: notification?.classId ?? null,
        testId: notification?.testId ?? null,
        attemptId: notification?.attemptId ?? null,
        achievementId: notification?.achievementId ?? null,
        createdAt: notification?.createdAt || "",
    };
}

// Получить список классов учителя
export async function listMyClasses() {
    const classes = await requestJson("/api/classes/my");
    if (!Array.isArray(classes)) {
        return [];
    }
    return classes.map(mapStudyClass);
}

// Получить класс по ID
export async function getMyClassById(classId) {
    const data = await requestJson(`/api/classes/my/${classId}`);
    return mapStudyClass(data);
}

// Получить список студентов класса
export async function listClassStudents(classId, page = 0, size = 100) {
    const data = await requestJson(`/api/classes/${classId}/students`, {
        params: { page, size },
    });
    const students = Array.isArray(data?.content) ? data.content : [];
    return {
        content: students.map(mapUser),
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}

// Получить заявки на вступление в класс
export async function listJoinRequests(classId) {
    const requests = await requestJson("/api/join-requests", {
        params: { classId },
    });
    if (!Array.isArray(requests)) {
        return [];
    }
    return requests.map(mapJoinRequest);
}

// Одобрить заявку на вступление
export async function approveJoinRequest(requestId, classId) {
    const data = await requestJson(`/api/join-requests/${requestId}/approve`, {
        method: "POST",
        params: { classId },
    });
    return mapUser(data);
}

// Отклонить заявку на вступление
export async function rejectJoinRequest(requestId, classId) {
    return requestJson(`/api/join-requests/${requestId}`, {
        method: "DELETE",
        params: { classId },
    });
}

// Удалить студента из класса
export async function removeStudentFromClass(classId, studentId) {
    return requestJson(`/api/classes/${classId}/students/${studentId}`, {
        method: "DELETE",
    });
}

// Отметить курс как завершенный для ученика в конкретном классе
export async function closeCourseForStudent(classId, studentId) {
    return requestJson(`/api/classes/${classId}/students/${studentId}/close-course`, {
        method: "POST",
    });
}

// Создать аккаунт студента
export async function createStudent(dto) {
    const created = await requestJson("/api/users/students", {
        method: "POST",
        body: dto,
    });
    return mapUser(created);
}

// Получить список попыток для проверки
export async function listPendingAttempts(options = {}) {
    const { courseId, activityId, classId, page = 0, size = 20 } = options;
    const data = await requestJson("/api/attempts/pending", {
        params: { courseId, activityId, classId, page, size },
    });
    return {
        content: Array.isArray(data?.content) ? data.content.map(mapPendingAttempt) : [],
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}

// Получить попытку по ID
export async function getAttempt(attemptId) {
    const data = await requestJson(`/api/attempts/${attemptId}`);
    return mapAttempt(data);
}

// Оценить попытку
export async function gradeAttempt(attemptId, gradeData) {
    const data = await requestJson(`/api/attempts/${attemptId}/grade`, {
        method: "PUT",
        body: gradeData,
    });
    return mapAttempt(data);
}

// Открыть активность для класса
export async function openActivityForClass(classId, activityId) {
    const url = `/api/classes/${classId}/activities/${activityId}/open`;
    console.log("[openActivityForClass] Sending request", { classId, activityId, url, method: "POST" });
    try {
        const result = await requestJson(url, {
            method: "POST",
        });
        console.log("[openActivityForClass] Request successful", { classId, activityId, result });
        return result;
    } catch (error) {
        console.error("[openActivityForClass] Request failed", { classId, activityId, error, status: error?.status, message: error?.message });
        throw error;
    }
}

// Открыть урок для класса
export async function openLessonForClass(classId, lessonId) {
    const url = `/api/classes/${classId}/lessons/${lessonId}/open`;
    console.log("[openLessonForClass] Sending request", { classId, lessonId, url, method: "POST" });
    try {
        const result = await requestJson(url, {
            method: "POST",
        });
        console.log("[openLessonForClass] Request successful", { classId, lessonId, result });
        return result;
    } catch (error) {
        console.error("[openLessonForClass] Request failed", { classId, lessonId, error, status: error?.status, message: error?.message });
        throw error;
    }
}

export async function listLessonsByCourse(courseId) {
    const lessons = await requestJson(`/api/courses/${courseId}/lessons`);
    if (!Array.isArray(lessons)) {
        return [];
    }
    return lessons.map(mapLesson);
}

export async function listActivitiesByLesson(lessonId) {
    const activities = await requestJson(`/api/lessons/${lessonId}/activities`);
    if (!Array.isArray(activities)) {
        return [];
    }
    return activities.map(mapActivity);
}

export async function listWeeklyActivitiesByCourse(courseId) {
    const activities = await requestJson(`/api/courses/${courseId}/activities/weekly`);
    if (!Array.isArray(activities)) {
        return [];
    }
    return activities.map(mapActivity);
}

// Получить статистику по классу
export async function getClassTopicStats(classId) {
    const stats = await requestJson(`/api/statistics/classes/${classId}/topics`);
    if (!Array.isArray(stats)) {
        return [];
    }
    return stats;
}

// Получить статистику по студенту
export async function getStudentTopicStats(studentId, courseId) {
    const stats = await requestJson(`/api/statistics/students/${studentId}/topics`, {
        params: { courseId },
    });
    if (!Array.isArray(stats)) {
        return [];
    }
    return stats;
}

// Получить список попыток по активности
export async function listActivityAttempts(activityId) {
    const attempts = await requestJson(`/api/activities/${activityId}/attempts`);
    if (!Array.isArray(attempts)) {
        return [];
    }
    return attempts;
}

// Получить активность по ID (с вопросами для преподавателя)
export async function getActivityById(activityId) {
    return requestJson(`/api/activities/${activityId}`);
}

// Получить достижения курса (доступные для назначения)
export async function listAchievementsByCourse(courseId) {
    const achievements = await requestJson(`/api/courses/${courseId}/achievements`);
    if (!Array.isArray(achievements)) {
        return [];
    }
    return achievements.map(mapAchievement);
}

// Получить уже назначенные достижения ученика
export async function listStudentAchievements(studentId) {
    const rows = await requestJson(`/api/students/${studentId}/achievements`);
    if (!Array.isArray(rows)) {
        return [];
    }
    return rows.map(mapStudentAchievement);
}

// Назначить достижение ученику
export async function awardAchievementToStudent(achievementId, studentId) {
    const row = await requestJson(`/api/achievements/${achievementId}/award/${studentId}`, {
        method: "POST",
    });
    return mapStudentAchievement(row);
}

// Снять достижение у ученика
export async function revokeAchievementFromStudent(achievementId, studentId) {
    return requestJson(`/api/achievements/${achievementId}/award/${studentId}`, {
        method: "DELETE",
    });
}

// Получить уведомления текущего преподавателя
export async function listMyNotifications(options = {}) {
    const { page = 0, size = 50 } = options;
    const data = await requestJson("/api/me/notifications", {
        params: { page, size },
    });
    const rows = Array.isArray(data?.content) ? data.content : [];
    return {
        content: rows.map(mapNotification),
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
        pageNumber: data?.pageNumber ?? 0,
    };
}

// Получить число непрочитанных уведомлений
export async function getMyUnreadNotificationsCount() {
    const data = await requestJson("/api/me/notifications/unread-count");
    return Number(data?.unread ?? 0);
}

// Отметить уведомление как прочитанное
export async function markNotificationRead(notificationId) {
    const data = await requestJson(`/api/me/notifications/${notificationId}/read`, {
        method: "PATCH",
    });
    return mapNotification(data);
}

// Отметить все уведомления как прочитанные
export async function markAllNotificationsRead() {
    const data = await requestJson("/api/me/notifications/read-all", {
        method: "PATCH",
    });
    return Number(data?.marked ?? 0);
}

// Получить профиль текущего пользователя
export async function getMyProfile() {
    const data = await requestJson("/api/me");
    return mapUser(data);
}

// Обновить профиль текущего пользователя
export async function updateMyProfile(dto) {
    const data = await requestJson("/api/me/profile", {
        method: "PATCH",
        body: dto,
    });
    return mapUser(data);
}

// Загрузить аватар текущего пользователя
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

    const data = await response.json();
    return mapUser(data);
}

// Удалить аватар текущего пользователя
export async function deleteMyAvatar() {
    const data = await requestJson("/api/me/avatar", {
        method: "DELETE",
    });
    return mapUser(data);
}
