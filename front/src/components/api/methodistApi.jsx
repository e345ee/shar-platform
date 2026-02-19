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
    const rawText = text ? text.trim() : "";
    let payload = null;
    if (rawText) {
        try {
            payload = JSON.parse(rawText);
        } catch (e) {
            payload = null;
        }
    }

    if (!response.ok) {
        const errorMessage =
            payload?.message
            || payload?.error
            || rawText
            || response.statusText
            || "Request failed";
        const error = new Error(errorMessage);
        error.status = response.status;
        error.payload = payload;
        error.rawText = rawText;
        throw error;
    }

    return payload;
}

function mapTeacher(user) {
    return {
        id: user?.id,
        roleId: user?.roleId ?? null,
        name: user?.name || "",
        email: user?.email || "",
        bio: user?.bio || "",
        photo: user?.photo || "",
        tgId: user?.tgId || "",
    };
}

function mapTeacherStats(row) {
    return {
        teacherId: row?.teacherId ?? null,
        teacherName: row?.teacherName || "",
        teacherEmail: row?.teacherEmail || "",
        classesCount: row?.classesCount ?? 0,
        studentsCount: row?.studentsCount ?? 0,
        submittedAttemptsCount: row?.submittedAttemptsCount ?? 0,
        gradedAttemptsCount: row?.gradedAttemptsCount ?? 0,
        avgGradePercent: row?.avgGradePercent ?? null,
    };
}

function mapCourse(course) {
    return {
        id: course?.id,
        name: course?.name || "",
        description: course?.description || "",
        createdById: course?.createdById ?? null,
        createdByName: course?.createdByName || "",
    };
}

function mapStudyClass(sc) {
    return {
        id: sc?.id,
        name: sc?.name || "",
        courseId: sc?.courseId ?? null,
        teacherId: sc?.teacherId ?? null,
        teacherName: sc?.teacherName || "",
        joinCode: sc?.joinCode || "",
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
        assignedWeekStart: activity?.assignedWeekStart || "",
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

function mapActivityQuestion(question) {
    return {
        id: question?.id,
        testId: question?.testId ?? null,
        orderIndex: question?.orderIndex ?? null,
        questionText: question?.questionText || "",
        questionType: question?.questionType || "",
        points: question?.points ?? 1,
        option1: question?.option1 || "",
        option2: question?.option2 || "",
        option3: question?.option3 || "",
        option4: question?.option4 || "",
        correctOption: question?.correctOption ?? null,
        correctTextAnswer: question?.correctTextAnswer || "",
    };
}

function mapAchievement(achievement) {
    return {
        id: achievement?.id,
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

export async function listTeachers(page = 0, size = 100) {
    const data = await requestJson(`/api/users/teachers?page=${page}&size=${size}`);
    const teachers = Array.isArray(data?.content) ? data.content : [];
    return teachers.map(mapTeacher);
}

export async function listTeacherStatistics(methodistId) {
    const query = methodistId != null ? `?methodistId=${encodeURIComponent(methodistId)}` : "";
    const rows = await requestJson(`/api/statistics/teachers${query}`);
    if (!Array.isArray(rows)) {
        return [];
    }
    return rows.map(mapTeacherStats);
}

export async function downloadTeacherStatisticsCsv(methodistId) {
    const token = getAccessToken();
    const query = methodistId != null ? `?methodistId=${encodeURIComponent(methodistId)}` : "";
    const response = await fetch(`${API_BASE_URL}/api/statistics/teachers/export/csv${query}`, {
        method: "GET",
        headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            Accept: "text/csv",
        },
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
        const error = new Error(payload?.message || "Не удалось скачать CSV");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return response.blob();
}

export async function createTeacher(dto) {
    const created = await requestJson("/api/users/teachers", {
        method: "POST",
        body: dto,
    });
    return mapTeacher(created);
}

export async function deleteTeacher(teacherId) {
    return requestJson(`/api/users/teachers/${teacherId}`, {
        method: "DELETE",
    });
}

export async function listCourses() {
    const courses = await requestJson("/api/courses");
    if (!Array.isArray(courses)) {
        return [];
    }
    return courses.map(mapCourse);
}

export async function listMyCourses() {
    const [courses, me] = await Promise.all([listCourses(), getMyProfile()]);
    const myId = me?.id;
    if (!myId) {
        return courses;
    }
    return courses.filter((course) => course.createdById === myId);
}

export async function listMyClasses() {
    const classes = await requestJson("/api/classes/my");
    if (!Array.isArray(classes)) {
        return [];
    }
    return classes.map(mapStudyClass);
}

export async function openActivityForClass(classId, activityId) {
    return requestJson(`/api/classes/${classId}/activities/${activityId}/open`, {
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

export async function createLesson(courseId, dto) {
    const token = getAccessToken();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const formData = new FormData();
    formData.append("title", dto.title);
    formData.append("description", dto.description || "");
    if (dto.orderIndex !== undefined && dto.orderIndex !== null && dto.orderIndex !== "") {
        formData.append("orderIndex", String(dto.orderIndex));
    }
    formData.append("presentation", dto.presentation);

    const response = await fetch(`${API_BASE_URL}/api/courses/${courseId}/lessons`, {
        method: "POST",
        headers,
        body: formData,
    });

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
        const error = new Error(payload?.message || "Не удалось создать урок");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return mapLesson(payload);
}

export async function updateLesson(lessonId, dto) {
    const token = getAccessToken();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const formData = new FormData();
    formData.append("title", dto.title);
    formData.append("description", dto.description || "");
    if (dto.orderIndex !== undefined && dto.orderIndex !== null && dto.orderIndex !== "") {
        formData.append("orderIndex", String(dto.orderIndex));
    }
    if (dto.presentation) {
        formData.append("presentation", dto.presentation);
    }

    const response = await fetch(`${API_BASE_URL}/api/lessons/${lessonId}`, {
        method: "PUT",
        headers,
        body: formData,
    });

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
        const error = new Error(payload?.message || "Не удалось обновить урок");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return mapLesson(payload);
}

export async function deleteLesson(lessonId) {
    return requestJson(`/api/lessons/${lessonId}`, {
        method: "DELETE",
    });
}

export async function createClass(dto) {
    const created = await requestJson("/api/classes", {
        method: "POST",
        body: dto,
    });
    return mapStudyClass(created);
}

export async function updateClass(classId, dto) {
    const updated = await requestJson(`/api/classes/${classId}`, {
        method: "PUT",
        body: dto,
    });
    return mapStudyClass(updated);
}

export async function deleteClass(classId) {
    return requestJson(`/api/classes/${classId}`, {
        method: "DELETE",
    });
}

export async function getClassStudentsCount(classId) {
    const page = await requestJson(`/api/classes/${classId}/students?page=0&size=1`);
    return page?.totalElements ?? 0;
}

export async function createCourse(dto) {
    const created = await requestJson("/api/courses", {
        method: "POST",
        body: dto,
    });
    return mapCourse(created);
}

export async function createCourseActivity(courseId, dto) {
    const created = await requestJson(`/api/courses/${courseId}/activities`, {
        method: "POST",
        body: dto,
    });
    return mapActivity(created);
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

export async function getActivityById(activityId) {
    const activity = await requestJson(`/api/activities/${activityId}`);
    return {
        ...mapActivity(activity),
        questions: Array.isArray(activity?.questions)
            ? activity.questions.map(mapActivityQuestion)
            : [],
    };
}

export async function updateCourseActivity(activityId, dto) {
    const updated = await requestJson(`/api/activities/${activityId}`, {
        method: "PUT",
        body: dto,
    });
    return mapActivity(updated);
}

export async function publishActivity(activityId) {
    const published = await requestJson(`/api/activities/${activityId}/publish`, {
        method: "POST",
    });
    return mapActivity(published);
}

export async function assignWeeklyActivity(activityId, weekStart) {
    const assigned = await requestJson(`/api/activities/${activityId}/schedule-week`, {
        method: "POST",
        body: { weekStart },
    });
    return mapActivity(assigned);
}

export async function deleteCourseActivity(activityId) {
    return requestJson(`/api/activities/${activityId}`, {
        method: "DELETE",
    });
}

export async function createActivityQuestion(activityId, dto) {
    const created = await requestJson(`/api/activities/${activityId}/questions`, {
        method: "POST",
        body: dto,
    });
    return mapActivityQuestion(created);
}

export async function updateActivityQuestion(activityId, questionId, dto) {
    const updated = await requestJson(`/api/activities/${activityId}/questions/${questionId}`, {
        method: "PUT",
        body: dto,
    });
    return mapActivityQuestion(updated);
}

export async function deleteActivityQuestion(activityId, questionId) {
    return requestJson(`/api/activities/${activityId}/questions/${questionId}`, {
        method: "DELETE",
    });
}

export async function updateCourse(courseId, dto) {
    const updated = await requestJson(`/api/courses/${courseId}`, {
        method: "PUT",
        body: dto,
    });
    return mapCourse(updated);
}

export async function deleteCourse(courseId) {
    return requestJson(`/api/courses/${courseId}`, {
        method: "DELETE",
    });
}

export async function listAchievementsByCourse(courseId) {
    const achievements = await requestJson(`/api/courses/${courseId}/achievements`);
    if (!Array.isArray(achievements)) {
        return [];
    }
    return achievements.map(mapAchievement);
}

export async function createAchievement(courseId, dto) {
    const token = getAccessToken();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const formData = new FormData();
    formData.append("title", dto.title);
    formData.append("jokeDescription", dto.jokeDescription);
    formData.append("description", dto.description);
    formData.append("photo", dto.photo);

    const response = await fetch(`${API_BASE_URL}/api/courses/${courseId}/achievements`, {
        method: "POST",
        headers,
        body: formData,
    });

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
        const error = new Error(payload?.message || "Не удалось создать достижение");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return mapAchievement(payload);
}

export async function updateAchievement(achievementId, dto) {
    const token = getAccessToken();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const formData = new FormData();
    formData.append("title", dto.title);
    formData.append("jokeDescription", dto.jokeDescription);
    formData.append("description", dto.description);
    if (dto.photo) {
        formData.append("photo", dto.photo);
    }

    const response = await fetch(`${API_BASE_URL}/api/achievements/${achievementId}`, {
        method: "PUT",
        headers,
        body: formData,
    });

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
        const error = new Error(payload?.message || "Не удалось обновить достижение");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return mapAchievement(payload);
}

export async function deleteAchievement(achievementId) {
    return requestJson(`/api/achievements/${achievementId}`, {
        method: "DELETE",
    });
}

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
