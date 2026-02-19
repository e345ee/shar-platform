import { useEffect, useState } from "react";
import { HomeIcon, BookIcon, ActivitiesIcon } from "../../../svgs/MethodistSvg";
import "./StudyActivities.css";
import {
  getActivityById,
  getMyCoursePage,
  listMyCourses,
} from "../../api/studentApi";
import ActivityAttemptModal from "./ActivityAttemptModal";
import MyAttemptsModal from "./MyAttemptsModal";

const TYPE_SECTIONS = [
  { key: "HOMEWORK_TEST", title: "Домашние работы" },
  { key: "CONTROL_WORK", title: "Контрольные работы" },
  { key: "WEEKLY_STAR", title: "Еженедельные задания" },
  { key: "REMEDIAL_TASK", title: "Задания для отстающих" },
];

const ATTEMPT_STATUS_LABELS = {
  IN_PROGRESS: "В процессе",
  SUBMITTED: "Сдано",
  GRADED: "Проверено",
};

function StudyActivities({ onBackToMain }) {
  const [selectedCourseId, setSelectedCourseId] = useState(null);
  const [courses, setCourses] = useState([]);
  const [coursePage, setCoursePage] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [selectedActivity, setSelectedActivity] = useState(null);
  const [showAttemptModal, setShowAttemptModal] = useState(false);
  const [showMyAttemptsModal, setShowMyAttemptsModal] = useState(false);
  const [attemptsActivity, setAttemptsActivity] = useState(null);

  useEffect(() => {
    let isCancelled = false;

    const load = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const coursesData = await listMyCourses();
        if (!isCancelled) {
          setCourses(coursesData);
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось загрузить курсы");
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    load();
    return () => {
      isCancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedCourseId) {
      setCoursePage(null);
      return;
    }

    let isCancelled = false;

    const loadCoursePage = async () => {
      setIsLoading(true);
      setErrorMessage("");
      setCoursePage(null);
      try {
        const page = await getMyCoursePage(selectedCourseId);
        if (!isCancelled) {
          setCoursePage(page);
        }
      } catch (e) {
        if (!isCancelled) {
          const errorMsg = e?.message || "Не удалось загрузить активности курса";
          setErrorMessage(errorMsg);
          setCoursePage(null);
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    loadCoursePage();
    return () => {
      isCancelled = true;
    };
  }, [selectedCourseId]);


  const handleStartActivity = async (activityId) => {
    setErrorMessage("");
    try {
      const activity = await getActivityById(activityId);
      setSelectedActivity(activity);
      setShowAttemptModal(true);
    } catch (e) {
      const errorMsg = e?.message || "Не удалось загрузить активность";
      setErrorMessage(errorMsg);
    }
  };

  const handleCloseModal = () => {
    setShowAttemptModal(false);
    setSelectedActivity(null);
    if (selectedCourseId) {
      const loadCoursePage = async () => {
        try {
          const page = await getMyCoursePage(selectedCourseId);
          setCoursePage(page);
        } catch (e) {
          // Ignore
        }
      };
      loadCoursePage();
    }
  };

  const handleOpenMyAttempts = (activity) => {
    setAttemptsActivity(activity);
    setShowMyAttemptsModal(true);
  };

  const getActivityTypeLabel = (type) => {
    const labels = {
      HOMEWORK_TEST: "Домашнее задание (тест)",
      CONTROL_WORK: "Контрольная работа",
      WEEKLY_STAR: "Еженедельное задание",
      REMEDIAL_TASK: "Задача для отстающих",
    };
    return labels[type] || type;
  };

  const getSourceHint = (sourceKey) => {
    const labels = {
      lesson: "Открыто по уроку",
      weekly: "Подходит на эту неделю",
      remedial: "Подходит вам персонально",
      course: "Курсовая активность",
    };
    return labels[sourceKey] || "";
  };

  const getStartButtonLabel = (type) => {
    if (type === "CONTROL_WORK") return "Начать контрольную";
    if (type === "HOMEWORK_TEST") return "Начать домашнюю работу";
    if (type === "WEEKLY_STAR") return "Начать еженедельное задание";
    if (type === "REMEDIAL_TASK") return "Начать задачу";
    return "Начать выполнение";
  };

  const canStartActivity = (activity, latestAttempt) => {
    const type = activity?.activityType || activity?.type;
    const status = String(latestAttempt?.status || "").toUpperCase();
    const attemptNumber = Number(latestAttempt?.attemptNumber || 0);
    const reachedMaxAttempts = attemptNumber >= 2 && status !== "IN_PROGRESS";

    if (reachedMaxAttempts) {
      return false;
    }

    if (type === "CONTROL_WORK" && status === "GRADED") {
      return false;
    }
    return true;
  };

  const normalizeCourseActivities = (page) => {
    if (!page) return [];

    const normalized = [];

    const pushNormalized = (item, sourceKey) => {
      if (!item) return;
      const activity = item.activity || item;
      if (!activity || !activity.id) return;
      normalized.push({
        activity,
        latestAttempt: item.latestAttempt || null,
        sourceKey,
      });
    };

    if (Array.isArray(page.lessons)) {
      page.lessons.forEach((lessonBlock) => {
        if (Array.isArray(lessonBlock?.activities)) {
          lessonBlock.activities.forEach((item) => pushNormalized(item, "lesson"));
        }
      });
    }

    if (Array.isArray(page.weeklyThisWeek)) {
      page.weeklyThisWeek.forEach((item) => pushNormalized(item, "weekly"));
    }

    if (Array.isArray(page.remedialThisWeek)) {
      page.remedialThisWeek.forEach((item) => pushNormalized(item, "remedial"));
    }

    if (Array.isArray(page.activities)) {
      page.activities.forEach((item) => pushNormalized(item, "course"));
    }

    // Убираем дубликаты активностей, сохраняя максимально полезные данные.
    const dedupedMap = new Map();
    normalized.forEach((item) => {
      const key = String(item.activity.id);
      const existing = dedupedMap.get(key);
      if (!existing) {
        dedupedMap.set(key, item);
        return;
      }
      if (!existing.latestAttempt && item.latestAttempt) {
        dedupedMap.set(key, item);
      }
    });

    return Array.from(dedupedMap.values());
  };

  const groupedActivities = (() => {
    const all = normalizeCourseActivities(coursePage);
    const map = new Map(TYPE_SECTIONS.map((section) => [section.key, []]));
    all.forEach((item) => {
      const type = item.activity?.activityType || item.activity?.type;
      if (map.has(type)) {
        map.get(type).push(item);
      }
    });
    return TYPE_SECTIONS.map((section) => ({
      ...section,
      items: map.get(section.key) || [],
    })).filter((section) => section.items.length > 0);
  })();

  return (
      <div className="study-activities-management">
        <div className="study-activities-container">
          <header className="study-activities-header">
            <div className="study-activities-header-left">
              <div>
                <h1 className="study-activities-title">
                  Выполнение учебной активности
                </h1>
                <p className="study-activities-subtitle">
                  Выполнение учебных активностей: тесты, домашние задания,
                  еженедельные задания, задачи для отстающих учеников
                </p>
              </div>
            </div>
            <div className="study-activities-header-right">
              <button
                  className="btn-home"
                  onClick={onBackToMain}
                  type="button"
              >
                <HomeIcon />
                На главную
              </button>
            </div>
          </header>

          {errorMessage && (
              <div className="study-activities-empty">
                {errorMessage.includes("fetch") || errorMessage.includes("Failed")
                    ? "Данные временно недоступны."
                    : errorMessage}
              </div>
          )}

          <div className="study-activities-card">
            <h3>Выберите курс</h3>
            <p className="study-activities-muted">
              Выберите курс для просмотра доступных активностей
            </p>
            {isLoading ? (
                <div className="study-activities-empty">Загрузка...</div>
            ) : (
                <div className="study-activities-course-list">
                  {courses.length > 0 ? (
                      courses.map((course) => (
                          <button
                              key={course.id}
                              className={`study-activities-course-item ${
                                  selectedCourseId === course.id ? "active" : ""
                              }`}
                              onClick={() => setSelectedCourseId(course.id)}
                              type="button"
                          >
                            <div className="study-activities-course-info">
                              <h4>
                                <BookIcon />
                                {course.name || `Курс #${course.id}`}
                              </h4>
                              <p>{course.description || "Без описания"}</p>
                            </div>
                          </button>
                      ))
                  ) : (
                      <button
                          className="study-activities-course-item study-activities-example"
                          type="button"
                          disabled
                      >
                        <div className="study-activities-course-info">
                          <h4>
                            <BookIcon />
                            Основы программирования
                          </h4>
                          <p>Изучение базовых концепций программирования</p>
                        </div>
                      </button>
                  )}
                </div>
            )}
          </div>

          {selectedCourseId && isLoading && (
              <div className="study-activities-empty">Загрузка активностей курса...</div>
          )}

          {/* Показываем активности выбранного курса */}
          {coursePage && selectedCourseId && (
              <div className="study-activities-list-section">
                <div className="study-activities-list-header">
                  <h2 className="study-activities-list-title">
                    Активности курса: {coursePage.course?.name || coursePage.courseName || "-"}
                  </h2>
                  <p className="study-activities-list-subtitle">
                    Доступные активности для выполнения
                  </p>
                </div>
                {groupedActivities.length > 0 ? (
                    groupedActivities.map((section) => (
                        <div key={section.key} className="study-activities-type-group">
                          <h3 className="study-activities-type-title">{section.title}</h3>
                          <div className="study-activities-list">
                            {section.items.map(({ activity, latestAttempt, sourceKey }) => (
                                <div key={activity.id} className="study-activity-card">
                                  <div className="study-activity-info">
                                    <h3 className="study-activity-title">
                                      <ActivitiesIcon />
                                      {activity.title || "Без названия"}
                                    </h3>
                                    <p className="study-activity-description">
                                      {activity.description || "Без описания"}
                                    </p>
                                    <div className="study-activity-meta">
                          <span className="study-activity-type">
                            {getActivityTypeLabel(activity.activityType || activity.type)}
                          </span>
                                      {getSourceHint(sourceKey) && (
                                          <span className="study-activity-suitability">
                                {getSourceHint(sourceKey)}
                              </span>
                                      )}
                                      {latestAttempt?.status && (
                                          <span className="study-activity-attempt-status">
                                Статус: {ATTEMPT_STATUS_LABELS[latestAttempt.status] || latestAttempt.status}
                              </span>
                                      )}
                                      {activity.deadline && (
                                          <span className="study-activity-deadline">
                              Дедлайн:{" "}
                                            {new Date(activity.deadline).toLocaleDateString(
                                                "ru-RU"
                                            )}
                            </span>
                                      )}
                                    </div>
                                    <div className="study-activity-actions">
                                      {canStartActivity(activity, latestAttempt) ? (
                                          <button
                                              className="study-activity-start-btn"
                                              onClick={() => handleStartActivity(activity.id)}
                                              type="button"
                                          >
                                            {getStartButtonLabel(activity.activityType || activity.type)}
                                          </button>
                                      ) : (
                                          <div className="study-activity-locked-note">
                                            {Number(latestAttempt?.attemptNumber || 0) >= 2
                                                ? "Лимит попыток достигнут (максимум 2)"
                                                : "Контрольная уже проверена, повторный запуск недоступен"}
                                          </div>
                                      )}
                                      <button
                                          className="study-activity-my-attempts-btn"
                                          onClick={() => handleOpenMyAttempts(activity)}
                                          type="button"
                                      >
                                        Мои попытки
                                      </button>
                                    </div>
                                  </div>
                                </div>
                            ))}
                          </div>
                        </div>
                    ))
                ) : (
                    <div className="study-activities-empty">
                      В этом курсе пока нет доступных активностей
                    </div>
                )}
              </div>
          )}

          {selectedCourseId && !isLoading && !coursePage && (
              <div className="study-activities-empty">
                {errorMessage || "Выберите курс для просмотра активностей"}
              </div>
          )}

          {!selectedCourseId && !isLoading && courses.length === 0 && (
              <div className="study-activities-empty">
                У вас пока нет доступных курсов
              </div>
          )}

          {!selectedCourseId && !isLoading && courses.length === 0 && (
              <div className="study-activities-list-section">
                <div className="study-activities-list-header">
                  <h2 className="study-activities-list-title">
                    Активности курса: Основы программирования
                  </h2>
                  <p className="study-activities-list-subtitle">
                    Доступные активности для выполнения
                  </p>
                </div>
                <div className="study-activities-list">
                  <div className="study-activity-card study-activities-example">
                    <div className="study-activity-info">
                      <h3 className="study-activity-title">
                        <ActivitiesIcon />
                        Тест по основам программирования
                      </h3>
                      <p className="study-activity-description">
                        Проверьте свои знания по основным концепциям
                        программирования: переменные, циклы, условия.
                      </p>
                      <div className="study-activity-meta">
                    <span className="study-activity-type">
                      Домашнее задание (тест)
                    </span>
                        <span className="study-activity-deadline">
                      Дедлайн: 25 января 2024
                    </span>
                      </div>
                      <button
                          className="study-activity-start-btn"
                          type="button"
                          disabled
                      >
                        Начать выполнение
                      </button>
                    </div>
                  </div>
                </div>
              </div>
          )}
        </div>

        {showAttemptModal && selectedActivity && (
            <ActivityAttemptModal
                activity={selectedActivity}
                onClose={handleCloseModal}
            />
        )}
        {showMyAttemptsModal && attemptsActivity && (
            <MyAttemptsModal
                activity={attemptsActivity}
                onClose={() => {
                  setShowMyAttemptsModal(false);
                  setAttemptsActivity(null);
                }}
            />
        )}
      </div>
  );
}

export default StudyActivities;
