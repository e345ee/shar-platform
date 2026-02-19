const API_BASE_URL = process.env.REACT_APP_API_URL || "";
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

    const response = await fetch(url, {
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
    };
}

function mapJoinRequest(req) {
    return {
        id: req?.id,
        studentId: req?.studentId ?? null,
        studentName: req?.studentName || "",
        studentEmail: req?.studentEmail || "",
        classId: req?.classId ?? null,
        className: req?.className || "",
        createdAt: req?.createdAt || "",
        status: req?.status || "",
    };
}

function mapAttempt(attempt) {
    return {
        id: attempt?.id,
        activityId: attempt?.activityId ?? null,
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
        grade: attempt?.grade ?? null,
        maxGrade: attempt?.maxGrade ?? null,
        startedAt: attempt?.startedAt || "",
        submittedAt: attempt?.submittedAt || "",
        gradedAt: attempt?.gradedAt || "",
        answers: attempt?.answers || [],
    };
}

function mapPendingAttempt(attempt) {
    return {
        id: attempt?.id,
        activityId: attempt?.activityId ?? null,
        activityTitle: attempt?.activityTitle || "",
        activityType: attempt?.activityType || "",
        studentId: attempt?.studentId ?? null,
        studentName: attempt?.studentName || "",
        studentEmail: attempt?.studentEmail || "",
        classId: attempt?.classId ?? null,
        className: attempt?.className || "",
        courseId: attempt?.courseId ?? null,
        courseName: attempt?.courseName || "",
        submittedAt: attempt?.submittedAt || "",
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
    return requestJson(`/api/classes/${classId}/activities/${activityId}/open`, {
        method: "POST",
    });
}

// Открыть урок для класса
export async function openLessonForClass(classId, lessonId) {
    return requestJson(`/api/classes/${classId}/lessons/${lessonId}/open`, {
        method: "POST",
    });
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
