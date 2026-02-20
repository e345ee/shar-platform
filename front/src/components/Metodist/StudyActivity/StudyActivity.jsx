import { useEffect, useMemo, useState } from "react";
import "./StudyActivity.css";
import {
    ActivitiesIcon,
    PlusIcon,
    EditIcon,
    TrashIcon,
    HomeIcon,
} from "../../../svgs/MethodistSvg.jsx";
import {CalendarIcon, BookIcon, LocationIcon, DocumentIcon, TestIcon, QuestionsListIcon, CheckIcon } from "../../../svgs/ActivitySvg.jsx";
import AddStudyActivityModal from "./AddStudyActivityModal";
import TextModal from "./TextModal";
import TestModal from "./TestModal";
import {
    assignWeeklyActivity,
    createActivityQuestion,
    createCourseActivity,
    deleteCourseActivity,
    deleteActivityQuestion,
    getActivityById,
    listActivitiesByLesson,
    listMyCourses,
    listRemedialActivitiesByCourse,
    listLessonsByCourse,
    listWeeklyActivitiesByCourse,
    publishActivity,
    updateActivityQuestion,
    updateCourseActivity,
} from "../../api/methodistApi";

const ACTIVITY_TYPE_UI = {
    HOMEWORK_TEST: { format: "Домашнее задание", color: "blue" },
    CONTROL_WORK: { format: "Контрольная работа", color: "red" },
    WEEKLY_STAR: { format: "Еженедельное задание", color: "green" },
    REMEDIAL_TASK: { format: "Для отстающих", color: "orange" },
};

const ACTIVITY_FILTER_TABS = [
    { key: "ALL", label: "Все" },
    { key: "HOMEWORK_TEST", label: "Домашние" },
    { key: "CONTROL_WORK", label: "Контрольные" },
    { key: "WEEKLY_STAR", label: "Еженедельные" },
    { key: "REMEDIAL_TASK", label: "Для отстающих" },
];

function formatDate(deadline) {
    if (!deadline) return "";
    const parsed = new Date(deadline);
    if (Number.isNaN(parsed.getTime())) return "";
    return parsed.toLocaleDateString("ru-RU", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
    });
}

function toIsoDateLocal(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function getCurrentWeekMondayIso() {
    const now = new Date();
    const normalized = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const dayIndexFromMonday = (normalized.getDay() + 6) % 7;
    normalized.setDate(normalized.getDate() - dayIndexFromMonday);
    return toIsoDateLocal(normalized);
}

function mapActivityToCard(activity, courses) {
    const uiMeta = ACTIVITY_TYPE_UI[activity.activityType] || {
        format: activity.activityType || "Активность",
        color: "blue",
    };
    const courseName = courses.find((course) => course.id === activity.courseId)?.name || "";
    return {
        id: activity.id,
        courseId: activity.courseId ?? null,
        title: activity.title,
        topic: activity.topic,
        format: uiMeta.format,
        class: courseName,
        date: formatDate(activity.deadline),
        description: activity.description || "",
        questionsCount: activity.questionCount || 0,
        color: uiMeta.color,
        activityType: activity.activityType,
        status: activity.status || "",
        deadline: activity.deadline,
        assignedWeekStart: activity.assignedWeekStart || "",
        tasks: [],
        questions: [],
    };
}

function StudyActivity({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [showTextTasksModal, setShowTextTasksModal] = useState(false);
    const [showTestQuestionsModal, setShowTestQuestionsModal] = useState(false);
    const [selectedActivity, setSelectedActivity] = useState(null);
    const [activities, setActivities] = useState([]);
    const [courses, setCourses] = useState([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isPublishing, setIsPublishing] = useState(false);
    const [isAssigningWeek, setIsAssigningWeek] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [modalMode, setModalMode] = useState("create");
    const [editingActivity, setEditingActivity] = useState(null);
    const [activeFilter, setActiveFilter] = useState("ALL");

    useEffect(() => {
        let isCancelled = false;
        setErrorMessage("");
        listMyCourses()
            .then((coursesData) => {
                if (isCancelled) return;
                setCourses(coursesData);
            })
            .catch((error) => {
                if (isCancelled) return;
                setErrorMessage(error?.message || "Не удалось загрузить данные");
            });
        return () => {
            isCancelled = true;
        };
    }, []);

    useEffect(() => {
        if (!courses.length) {
            setActivities([]);
            return;
        }

        let isCancelled = false;
        setErrorMessage("");
        (async () => {
            try {
                const collected = [];
                for (const course of courses) {
                    const lessons = await listLessonsByCourse(course.id);
                    const perLesson = await Promise.all(
                        lessons.map((lesson) => listActivitiesByLesson(lesson.id))
                    );
                    perLesson.flat().forEach((activity) => collected.push(activity));

                    const weekly = await listWeeklyActivitiesByCourse(course.id);
                    weekly.forEach((activity) => collected.push(activity));

                    const remedial = await listRemedialActivitiesByCourse(course.id);
                    remedial.forEach((activity) => collected.push(activity));
                }

                const dedup = new Map();
                for (const activity of collected) {
                    if (!dedup.has(activity.id)) {
                        dedup.set(activity.id, activity);
                    }
                }

                const cards = Array.from(dedup.values())
                    .map((activity) => mapActivityToCard(activity, courses))
                    .sort((a, b) => (b.id || 0) - (a.id || 0));

                if (!isCancelled) {
                    setActivities(cards);
                }
            } catch (error) {
                if (!isCancelled) {
                    setErrorMessage(error?.message || "Не удалось загрузить активности");
                }
            }
        })();

        return () => {
            isCancelled = true;
        };
    }, [courses]);

    const stats = {
        controlWorks: activities.filter((a) => a.activityType === "CONTROL_WORK").length,
        homework: activities.filter(a => a.format === "Домашнее задание").length,
        weekly: activities.filter(a => a.format === "Еженедельное задание").length,
        lagging: activities.filter(a => a.format === "Для отстающих").length,
    };

    const totalActivities = activities.length;
    const filteredActivities = useMemo(() => {
        if (activeFilter === "ALL") {
            return activities;
        }
        return activities.filter((activity) => activity.activityType === activeFilter);
    }, [activities, activeFilter]);

    const handleSubmitActivity = async ({ id, courseId, payload }) => {
        setIsSubmitting(true);
        setErrorMessage("");
        try {
            if (modalMode === "edit" && id) {
                const updated = await updateCourseActivity(id, payload);
                const updatedCard = mapActivityToCard(updated, courses);
                setActivities((prev) =>
                    prev.map((activity) => (activity.id === id ? { ...activity, ...updatedCard } : activity))
                );
            } else {
                const created = await createCourseActivity(courseId, payload);
                const newActivity = mapActivityToCard(created, courses);
                setActivities((prev) => [newActivity, ...prev]);
            }
        } catch (error) {
            if (modalMode === "edit" && error?.status === 400) {
                setErrorMessage("Опубликованный тест нельзя редактировать");
                throw error;
            }
            setErrorMessage(
                error?.message || (modalMode === "edit" ? "Не удалось обновить активность" : "Не удалось создать активность")
            );
            throw error;
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleOpenCreateModal = () => {
        setErrorMessage("");
        setModalMode("create");
        setEditingActivity(null);
        setShowModal(true);
    };

    const handleOpenEditActivity = (activity) => {
        setErrorMessage("");
        if (activity?.status === "READY") {
            setErrorMessage("Опубликованный тест нельзя редактировать");
            return;
        }
        getActivityById(activity.id)
            .then((details) => {
                setModalMode("edit");
                setEditingActivity(details);
                setShowModal(true);
            })
            .catch((error) => {
                setErrorMessage(error?.message || "Не удалось загрузить активность для редактирования");
            });
    };

    const handleOpenTextTasks = (activity) => {
        setSelectedActivity(activity);
        setShowTextTasksModal(true);
    };

    const handleSaveTasks = (activityId, tasks) => {
        setActivities(activities.map(activity =>
            activity.id === activityId
                ? { ...activity, tasks, tasksCount: tasks.length }
                : activity
        ));
    };

    const handleOpenTestQuestions = (activity) => {
        setErrorMessage("");
        getActivityById(activity.id)
            .then((details) => {
                const questions = (details.questions || []).map((q) => ({
                    id: q.id,
                    text: q.questionText || "",
                    questionType: q.questionType || "SINGLE_CHOICE",
                    points: q.points || 1,
                    options: [
                        { id: 1, text: q.option1 || "" },
                        { id: 2, text: q.option2 || "" },
                        { id: 3, text: q.option3 || "" },
                        { id: 4, text: q.option4 || "" },
                    ],
                    correctAnswer: q.correctOption || null,
                    correctTextAnswer: q.correctTextAnswer || "",
                }));
                setSelectedActivity({ ...activity, questions });
                setShowTestQuestionsModal(true);
            })
            .catch((error) => {
                setErrorMessage(error?.message || "Не удалось загрузить вопросы");
            });
    };

    const buildQuestionPayload = (question, index, activityType) => {
        const questionType = question.questionType || "SINGLE_CHOICE";
        const points = Number(question.points || 1);
        if (!Number.isFinite(points) || points < 1) {
            throw new Error(`Некорректные баллы в вопросе ${index + 1}`);
        }

        const text = (question.text || "").trim();
        if (!text) {
            throw new Error(`Заполните текст вопроса ${index + 1}`);
        }

        if (questionType === "OPEN") {
            if (activityType === "REMEDIAL_TASK") {
                throw new Error("Для REMEDIAL_TASK вопросы типа OPEN недоступны");
            }
            return {
                orderIndex: index + 1,
                questionText: text,
                questionType: "OPEN",
                points,
                option1: null,
                option2: null,
                option3: null,
                option4: null,
                correctOption: null,
                correctTextAnswer: null,
            };
        }

        if (questionType === "TEXT") {
            const correctTextAnswer = (question.correctTextAnswer || "").trim();
            if (!correctTextAnswer) {
                throw new Error(`Заполните правильный текстовый ответ в вопросе ${index + 1}`);
            }
            return {
                orderIndex: index + 1,
                questionText: text,
                questionType: "TEXT",
                points,
                option1: null,
                option2: null,
                option3: null,
                option4: null,
                correctOption: null,
                correctTextAnswer,
            };
        }

        const options = Array.isArray(question.options) ? question.options.slice(0, 4) : [];
        if (options.length < 4) {
            throw new Error(`В вопросе ${index + 1} должно быть 4 варианта ответа`);
        }
        const optionValues = options.map((option) => (option?.text || "").trim());
        if (optionValues.some((value) => !value)) {
            throw new Error(`Заполните все варианты в вопросе ${index + 1}`);
        }
        if (!question.correctAnswer) {
            throw new Error(`Выберите правильный ответ в вопросе ${index + 1}`);
        }

        const correctIndex = options.findIndex((option) => option.id === question.correctAnswer);
        if (correctIndex < 0 || correctIndex > 3) {
            throw new Error(`Некорректный правильный ответ в вопросе ${index + 1}`);
        }

        return {
            orderIndex: index + 1,
            questionText: text,
            questionType: "SINGLE_CHOICE",
            points,
            option1: optionValues[0],
            option2: optionValues[1],
            option3: optionValues[2],
            option4: optionValues[3],
            correctOption: correctIndex + 1,
            correctTextAnswer: null,
        };
    };

    const handleSaveQuestions = async (activityId, questions) => {
        setErrorMessage("");
        const details = await getActivityById(activityId);
        const activityType = details?.activityType || "";
        const existingQuestions = Array.isArray(details.questions) ? details.questions : [];
        const existingIds = new Set(existingQuestions.map((q) => q.id));

        const prepared = questions.map((question, idx) => ({
            id: question.id,
            payload: buildQuestionPayload(question, idx, activityType),
        }));

        const keptExistingIds = new Set(
            prepared.filter((item) => existingIds.has(item.id)).map((item) => item.id)
        );

        const toDelete = existingQuestions.filter((q) => !keptExistingIds.has(q.id));
        for (const q of toDelete) {
            await deleteActivityQuestion(activityId, q.id);
        }

        for (const item of prepared) {
            if (existingIds.has(item.id)) {
                await updateActivityQuestion(activityId, item.id, item.payload);
            } else {
                await createActivityQuestion(activityId, item.payload);
            }
        }

        const refreshed = await getActivityById(activityId);
        const refreshedQuestions = (refreshed.questions || []).map((q) => ({
            id: q.id,
            text: q.questionText || "",
            questionType: q.questionType || "SINGLE_CHOICE",
            points: q.points || 1,
            options: [
                { id: 1, text: q.option1 || "" },
                { id: 2, text: q.option2 || "" },
                { id: 3, text: q.option3 || "" },
                { id: 4, text: q.option4 || "" },
            ],
            correctAnswer: q.correctOption || null,
            correctTextAnswer: q.correctTextAnswer || "",
        }));

        setActivities((prev) =>
            prev.map((activity) =>
                activity.id === activityId
                    ? { ...activity, questions: refreshedQuestions, questionsCount: refreshed.questionCount || 0 }
                    : activity
            )
        );
        setSelectedActivity((prev) =>
            prev && prev.id === activityId
                ? { ...prev, questions: refreshedQuestions, questionsCount: refreshed.questionCount || 0 }
                : prev
        );
    };

    const handleDeleteActivity = async (id) => {
        setErrorMessage("");
        if (!id) {
            setErrorMessage("Не удалось определить активность для удаления");
            return;
        }
        const targetActivity = activities.find((activity) => activity.id === id);
        if (targetActivity?.status === "READY") {
            setErrorMessage("Тест уже опубликован!");
            return;
        }
        try {
            await deleteCourseActivity(id);
            setActivities((prev) => prev.filter((activity) => activity.id !== id));
        } catch (error) {
            if (error?.status === 400) {
                setErrorMessage("Тест уже опубликован!");
                return;
            }
            const httpCode = error?.status ? ` (HTTP ${error.status})` : "";
            setErrorMessage((error?.message || "Не удалось удалить активность") + httpCode);
        }
    };

    const handlePublishActivity = async (activityId) => {
        setIsPublishing(true);
        setErrorMessage("");
        try {
            const published = await publishActivity(activityId);
            const updatedCard = mapActivityToCard(published, courses);
            setActivities((prev) =>
                prev.map((activity) => (activity.id === activityId ? { ...activity, ...updatedCard } : activity))
            );
            setSelectedActivity((prev) =>
                prev && prev.id === activityId ? { ...prev, ...updatedCard } : prev
            );
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось опубликовать активность");
        } finally {
            setIsPublishing(false);
        }
    };

    const handleAssignWeeklyActivity = async (activityId) => {
        const weekStart = getCurrentWeekMondayIso();
        setIsAssigningWeek(true);
        setErrorMessage("");
        try {
            const assigned = await assignWeeklyActivity(activityId, weekStart);
            const updatedCard = mapActivityToCard(assigned, courses);
            setActivities((prev) =>
                prev.map((activity) => (activity.id === activityId ? { ...activity, ...updatedCard } : activity))
            );
            setSelectedActivity((prev) =>
                prev && prev.id === activityId ? { ...prev, ...updatedCard } : prev
            );
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось назначить еженедельное задание");
        } finally {
            setIsAssigningWeek(false);
        }
    };

    const getActivityTypeTag = (format) => {
        const formatColors = {
            "Контрольная работа": "red",
            "Домашнее задание": "blue",
            "Еженедельное задание": "green",
            "Для отстающих": "orange",
        };
        return {
            format: { text: format, color: formatColors[format] || "gray" },
        };
    };

    return (
        <div className="activities-management">
            <div className="activities-container">
                <header className="activities-header">
                    <div className="activities-header-left">
                        <div className="activities-header-icon">
                            <ActivitiesIcon />
                        </div>
                        <div>
                            <h1 className="activities-title">Учебные активности</h1>
                            <p className="activities-subtitle">Создание и управление учебными заданиями</p>
                        </div>
                    </div>
                    <div className="activities-header-actions">
                        <button className="btn-add-activity" onClick={handleOpenCreateModal} type="button">
                            <PlusIcon />
                            Создать активность
                        </button>
                        <button className="btn-home" onClick={onBackToMain} type="button">
                            <HomeIcon />
                            На главную
                        </button>
                    </div>
                </header>

                <div className="activities-stats">
                    <div className="stat-card stat-red">
                        <div className="stat-icon">
                            <TestIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Контрольные работы</div>
                            <div className="stat-value">{stats.controlWorks}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-blue">
                        <div className="stat-icon">
                            <BookIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Домашние задания</div>
                            <div className="stat-value">{stats.homework}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-green">
                        <div className="stat-icon">
                            <CalendarIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Еженедельные</div>
                            <div className="stat-value">{stats.weekly}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-orange">
                        <div className="stat-icon">
                            <BookIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Для отстающих</div>
                            <div className="stat-value">{stats.lagging}</div>
                        </div>
                    </div>
                </div>

                <section className="activities-list-section">
                    <div className="activities-list-header">
                        <h2 className="activities-list-title">Список активностей</h2>
                        <p className="activities-list-subtitle">
                            Всего активностей: {totalActivities}
                        </p>
                    </div>
                    <div className="activities-filters">
                        {ACTIVITY_FILTER_TABS.map((tab) => (
                            <button
                                key={tab.key}
                                type="button"
                                className={`activities-filter-btn ${activeFilter === tab.key ? "active" : ""}`}
                                onClick={() => setActiveFilter(tab.key)}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>
                    {errorMessage && <div className="activity-error">{errorMessage}</div>}
                    <div className="activities-list">
                        {filteredActivities.length === 0 && (
                            <div className="activity-empty">Активностей пока нет</div>
                        )}
                        {filteredActivities.map((activity) => {
                            const tags = getActivityTypeTag(activity.format);
                            return (
                                <div key={activity.id} className={`activity-card activity-card-${activity.color}`}>
                                    <div className={`activity-icon activity-icon-${activity.color}`}>
                                        <BookIcon />
                                    </div>
                                    <div className="activity-info">
                                        <div className="activity-header-info">
                                            <h3 className="activity-title">{activity.title}</h3>
                                            <div className="activity-tags">
                                                <span className={`activity-tag tag-${tags.format.color}`}>
                                                    {tags.format.text}
                                                </span>
                                            </div>
                                        </div>
                                        <div className="activity-details">
                                            <div className="activity-detail-item">
                                                <LocationIcon />
                                                <span>{activity.topic}</span>
                                            </div>
                                            <div className="activity-detail-item">
                                                <BookIcon />
                                                <span>Класс: {activity.class}</span>
                                            </div>
                                            <div className="activity-detail-item">
                                                <CalendarIcon />
                                                <span>{activity.date}</span>
                                            </div>
                                            <div className="activity-detail-item">
                                                <DocumentIcon />
                                                <span>Статус: {activity.status || "неизвестно"}</span>
                                            </div>
                                            {activity.activityType === "WEEKLY_STAR" && activity.assignedWeekStart && (
                                                <div className="activity-detail-item">
                                                    <CalendarIcon />
                                                    <span>Назначено на неделю: {formatDate(activity.assignedWeekStart)}</span>
                                                </div>
                                            )}
                                        </div>
                                        <p className="activity-description">{activity.description}</p>
                                        <div className="activity-questions">
                                            <button
                                                className="btn-questions"
                                                type="button"
                                                onClick={() => handleOpenTestQuestions(activity)}
                                            >
                                                <QuestionsListIcon />
                                                {activity.questionsCount || 0} вопросов
                                            </button>
                                        </div>
                                    </div>
                                    <div className="activity-actions">
                                        {activity.activityType === "WEEKLY_STAR" && activity.status === "READY" && (
                                            <button
                                                className="activity-action-btn btn-schedule-week"
                                                type="button"
                                                aria-label="Schedule week"
                                                onClick={() => handleAssignWeeklyActivity(activity.id)}
                                                disabled={isAssigningWeek}
                                                title={isAssigningWeek ? "Назначение..." : "Назначить на текущую неделю"}
                                            >
                                                <CalendarIcon />
                                            </button>
                                        )}
                                        {activity.status !== "READY" && (
                                            <button
                                                className="activity-action-btn btn-ready"
                                                type="button"
                                                aria-label="Publish"
                                                onClick={() => handlePublishActivity(activity.id)}
                                                disabled={isPublishing}
                                                title={isPublishing ? "Публикация..." : "Опубликовать активность"}
                                            >
                                                <CheckIcon />
                                            </button>
                                        )}
                                        <button
                                            className="activity-action-btn"
                                            type="button"
                                            aria-label="Edit"
                                            onClick={() => handleOpenEditActivity(activity)}
                                            disabled={activity.status === "READY"}
                                            title={activity.status === "READY" ? "Опубликованную активность нельзя редактировать" : "Редактировать"}
                                        >
                                            <EditIcon />
                                        </button>
                                        <button
                                            className="activity-action-btn"
                                            type="button"
                                            onClick={() => handleDeleteActivity(activity.id)}
                                            aria-label="Delete"
                                        >
                                            <TrashIcon />
                                        </button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>
            </div>

            <AddStudyActivityModal
                isOpen={showModal}
                onClose={() => {
                    setShowModal(false);
                    setModalMode("create");
                    setEditingActivity(null);
                }}
                onSubmitActivity={handleSubmitActivity}
                courses={courses}
                errorMessage={errorMessage}
                isSubmitting={isSubmitting}
                mode={modalMode}
                initialActivity={editingActivity}
            />
            <TextModal
                isOpen={showTextTasksModal}
                onClose={() => {
                    setShowTextTasksModal(false);
                    setSelectedActivity(null);
                }}
                activity={selectedActivity}
                onSaveTasks={handleSaveTasks}
            />
            <TestModal
                isOpen={showTestQuestionsModal}
                onClose={() => {
                    setShowTestQuestionsModal(false);
                    setSelectedActivity(null);
                }}
                activity={selectedActivity}
                onSaveQuestions={handleSaveQuestions}
            />
        </div>
    );
}

export default StudyActivity;
