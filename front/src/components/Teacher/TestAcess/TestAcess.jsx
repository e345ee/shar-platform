import { useEffect, useMemo, useState } from "react";
import "./TestAcess.css";
import { HomeIcon } from "../../../svgs/TeacherSvg.jsx";
import {
  LockIcon,
  ManageIcon,
  OpenLockIcon,
  ClosedLockIcon,
} from "../../../svgs/TestSvg.jsx";
import {
  CalendarIcon,
  DocumentIcon,
  TagIcon,
  LocationIcon,
} from "../../../svgs/ActivitySvg.jsx";
import TestAccessModal from "./TestAcessModal";
import {
  listActivitiesByLesson,
  listLessonsByCourse,
  listMyClasses,
  listWeeklyActivitiesByCourse,
  openActivityForClass,
} from "../../api/teacherApi";

const ACTIVITY_TYPE_UI = {
  HOMEWORK_TEST: { format: "Домашнее задание", color: "blue", type: "Тест" },
  CONTROL_WORK: { format: "Контрольная работа", color: "red", type: "Тест" },
  WEEKLY_STAR: { format: "Еженедельное задание", color: "green", type: "Тест" },
  REMEDIAL_TASK: { format: "Для отстающих", color: "orange", type: "Тест" },
};

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

function TestAccess({ onBackToMain }) {
  const [showModal, setShowModal] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState(null);
  const [activities, setActivities] = useState([]);
  const [classes, setClasses] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  const stats = useMemo(() => ({
    totalActivities: activities.length,
    totalClasses: classes.length,
    openAccesses: activities.reduce(
        (sum, activity) => sum + activity.openClasses.length,
        0,
    ),
  }), [activities, classes.length]);

  useEffect(() => {
    let isCancelled = false;
    setErrorMessage("");
    setIsLoading(true);
    (async () => {
      try {
        const classesData = await listMyClasses();
        if (isCancelled) return;
        const classCards = classesData.map((classItem) => ({
          id: classItem.id,
          name: classItem.name,
          subject: classItem.courseName || "Курс",
          courseId: classItem.courseId ?? null,
        }));
        setClasses(classCards);

        const courseIds = Array.from(
            new Set(classCards.map((classItem) => classItem.courseId).filter(Boolean)),
        );

        const collected = [];
        for (const courseId of courseIds) {
          const lessons = await listLessonsByCourse(courseId);
          const perLesson = await Promise.all(
              lessons.map((lesson) => listActivitiesByLesson(lesson.id)),
          );
          perLesson.flat().forEach((activity) => collected.push(activity));

          const weekly = await listWeeklyActivitiesByCourse(courseId);
          weekly.forEach((activity) => collected.push(activity));
        }

        const uniqueActivities = new Map();
        collected.forEach((activity) => {
          if (!uniqueActivities.has(activity.id)) {
            uniqueActivities.set(activity.id, activity);
          }
        });

        const cards = Array.from(uniqueActivities.values())
            .map((activity) => {
              const uiMeta = ACTIVITY_TYPE_UI[activity.activityType] || {
                format: activity.activityType || "Активность",
                color: "blue",
                type: "Тест",
              };
              return {
                id: activity.id,
                title: activity.title || "Без названия",
                topic: activity.topic || "Без темы",
                format: uiMeta.format,
                type: uiMeta.type,
                date: formatDate(activity.deadline),
                questionsCount: activity.questionCount || 0,
                openClasses: [],
                color: uiMeta.color,
                status: activity.status || "",
                courseId: activity.courseId ?? null,
              };
            })
            .sort((a, b) => (b.id || 0) - (a.id || 0));

        if (!isCancelled) {
          setActivities(cards);
        }
      } catch (error) {
        if (!isCancelled) {
          setErrorMessage(error?.message || "Не удалось загрузить данные");
          setActivities([]);
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    })();

    return () => {
      isCancelled = true;
    };
  }, []);

  const handleOpenModal = (activity) => {
    setErrorMessage("");
    setSelectedActivity(activity);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedActivity(null);
  };

  const handleToggleAccess = async (activityId, classId, isOpen) => {
    if (!isOpen) {
      setErrorMessage("Закрытие доступа пока не поддерживается.");
      return;
    }

    const targetActivity = activities.find((activity) => activity.id === activityId);
    if (!targetActivity) {
      setErrorMessage("Активность не найдена.");
      return;
    }
    if (targetActivity.status !== "READY") {
      setErrorMessage("Открыть доступ можно только для активности в статусе READY.");
      return;
    }

    const targetClass = classes.find((classItem) => classItem.id === classId);
    if (!targetClass) {
      setErrorMessage("Класс не найден.");
      return;
    }
    if (targetActivity.courseId && targetClass.courseId !== targetActivity.courseId) {
      setErrorMessage("Класс не относится к курсу выбранной активности.");
      return;
    }

    try {
      setErrorMessage("");
      await openActivityForClass(classId, activityId);
      setActivities((prev) =>
          prev.map((activity) =>
              activity.id !== activityId
                  ? activity
                  : {
                    ...activity,
                    openClasses: activity.openClasses.includes(classId)
                        ? activity.openClasses
                        : [...activity.openClasses, classId],
                  },
          ),
      );
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось открыть доступ к активности.");
    }
  };

  const getActivityTypeTag = (format, type) => {
    const formatColors = {
      "Контрольная работа": "red",
      "Домашнее задание": "blue",
      "Еженедельное задание": "green",
      "Для отстающих": "orange",
    };
    const typeColors = {
      Тест: "pink",
      Текст: "light-green",
    };
    return {
      format: { text: format, color: formatColors[format] || "gray" },
      type: { text: type, color: typeColors[type] || "gray" },
    };
  };

  return (
      <div className="test-access-management">
        <div className="test-access-container">
          <header className="test-access-header">
            <div className="test-access-header-left">
              <div className="test-access-header-icon">
                <LockIcon />
              </div>
              <div>
                <h1 className="test-access-title">
                  Управление доступом к тестам
                </h1>
                <p className="test-access-subtitle">
                  Открывайте и закрывайте доступ к активностям для классов
                </p>
              </div>
            </div>
            <div className="test-access-header-actions">
              <button className="btn-home" onClick={onBackToMain} type="button">
                <HomeIcon />
                На главную
              </button>
            </div>
          </header>

          <div className="test-access-stats">
            <div className="stat-card stat-purple">
              <div className="stat-icon">
                <DocumentIcon />
              </div>
              <div className="stat-content">
                <div className="stat-label">Всего активностей</div>
                <div className="stat-value">{stats.totalActivities}</div>
              </div>
            </div>
            <div className="stat-card stat-blue">
              <div className="stat-icon">
                <TagIcon />
              </div>
              <div className="stat-content">
                <div className="stat-label">Классов</div>
                <div className="stat-value">{stats.totalClasses}</div>
              </div>
            </div>
            <div className="stat-card stat-green">
              <div className="stat-icon">
                <OpenLockIcon />
              </div>
              <div className="stat-content">
                <div className="stat-label">Открытых доступов</div>
                <div className="stat-value">{stats.openAccesses}</div>
              </div>
            </div>
          </div>

          <section className="test-access-list-section">
            <div className="test-access-list-header">
              <h2 className="test-access-list-title">Активности</h2>
              <p className="test-access-list-subtitle">
                Нажмите на активность для управления доступом
              </p>
            </div>
            <div className="test-access-list">
              {errorMessage && <div className="activity-error">{errorMessage}</div>}
              {isLoading && <div className="activity-empty">Загрузка...</div>}
              {!isLoading && activities.length === 0 && (
                  <div className="activity-empty">Нет доступных активностей</div>
              )}
              {activities.map((activity) => {
                const tags = getActivityTypeTag(activity.format, activity.type);
                const isOpen = activity.openClasses.length > 0;
                const openClassNames = classes
                    .filter((classItem) => activity.openClasses.includes(classItem.id))
                    .map((classItem) => classItem.name);
                return (
                    <div key={activity.id} className="test-access-card">
                      <div className="test-access-card-content">
                        <div className="test-access-card-header">
                          <h3 className="test-access-card-title">
                            {activity.title}
                          </h3>
                          <div className="test-access-card-tags">
                        <span
                            className={`test-access-tag tag-${tags.format.color}`}
                        >
                          {tags.format.text}
                        </span>
                            <span
                                className={`test-access-tag tag-${tags.type.color}`}
                            >
                          {tags.type.text}
                        </span>
                          </div>
                        </div>
                        <div className="test-access-card-details">
                          <div className="test-access-detail-item">
                            <LocationIcon />
                            <span>{activity.topic}</span>
                          </div>
                          <div className="test-access-detail-item">
                            <CalendarIcon />
                            <span>{activity.date}</span>
                          </div>
                          <div className="test-access-detail-item">
                            <DocumentIcon />
                            <span>{activity.questionsCount} вопросов</span>
                          </div>
                          <div className="test-access-detail-item">
                            <DocumentIcon />
                            <span>Статус: {activity.status || "неизвестно"}</span>
                          </div>
                        </div>
                        <div className="test-access-status">
                          {isOpen ? (
                              <>
                                <OpenLockIcon />
                                <span>
                            Открыт для {activity.openClasses.length} класса
                          </span>
                                <span className="test-access-classes">
                            ({openClassNames.join(", ")})
                          </span>
                              </>
                          ) : (
                              <>
                                <ClosedLockIcon />
                                <span>Закрыт для всех классов</span>
                              </>
                          )}
                        </div>
                      </div>
                      <button
                          className="btn-manage-access"
                          onClick={() => handleOpenModal(activity)}
                          type="button"
                      >
                        <ManageIcon />
                        Управление
                      </button>
                    </div>
                );
              })}
            </div>
          </section>
        </div>

        <TestAccessModal
            isOpen={showModal}
            onClose={handleCloseModal}
            activity={selectedActivity}
            classes={classes.filter((classItem) =>
                selectedActivity?.courseId ? classItem.courseId === selectedActivity.courseId : true,
            )}
            onToggleAccess={handleToggleAccess}
        />
      </div>
  );
}

export default TestAccess;
