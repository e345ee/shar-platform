import { useEffect, useMemo, useState } from "react";
import "./TestAcess.css";
import { HomeIcon } from "../../../svgs/TeacherSvg.jsx";
import {
  LockIcon,
  ManageIcon,
  OpenLockIcon,
} from "../../../svgs/TestSvg.jsx";
import {
  CalendarIcon,
  DocumentIcon,
  TagIcon,
  LocationIcon,
} from "../../../svgs/ActivitySvg.jsx";
import {
  listActivitiesByLesson,
  listLessonsByCourse,
  listOpenClassIdsForActivity,
  listOpenClassIdsForLesson,
  listMyClasses,
  listWeeklyActivitiesByCourse,
  openActivityForClass,
  openLessonForClass,
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
  const [activities, setActivities] = useState([]);
  const [lessons, setLessons] = useState([]);
  const [classes, setClasses] = useState([]);
  const [openedLessonAccesses, setOpenedLessonAccesses] = useState({});
  const [openingLessonAccesses, setOpeningLessonAccesses] = useState({});
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  const stats = useMemo(() => ({
    totalActivities: activities.length,
    totalClasses: classes.length,
  }), [activities, classes.length]);

  const getLessonAccessKey = (lessonId, classId) => `${lessonId}-${classId}`;

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

        const collectedLessons = [];
        const collected = [];
        for (const courseId of courseIds) {
          const courseLessons = await listLessonsByCourse(courseId);
          courseLessons.forEach((lesson) => {
            collectedLessons.push({
              ...lesson,
              courseId,
            });
          });

          const perLesson = await Promise.all(
              courseLessons.map((lesson) => listActivitiesByLesson(lesson.id)),
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
          const cardsWithOpenClasses = await Promise.all(
              cards.map(async (card) => {
                try {
                  const openClassIds = await listOpenClassIdsForActivity(card.id);
                  return { ...card, openClasses: openClassIds };
                } catch {
                  return card;
                }
              }),
          );
          const sortedLessons = collectedLessons.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0));
          const lessonOpenPairs = await Promise.all(
              sortedLessons.map(async (lesson) => {
                try {
                  const openClassIds = await listOpenClassIdsForLesson(lesson.id);
                  return { lessonId: lesson.id, openClassIds };
                } catch {
                  return { lessonId: lesson.id, openClassIds: [] };
                }
              }),
          );
          const nextOpenedLessonAccesses = {};
          lessonOpenPairs.forEach(({ lessonId, openClassIds }) => {
            openClassIds.forEach((classId) => {
              nextOpenedLessonAccesses[getLessonAccessKey(lessonId, classId)] = true;
            });
          });
          setOpenedLessonAccesses(nextOpenedLessonAccesses);
          setLessons(sortedLessons);
          setActivities(cardsWithOpenClasses);
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

  const handleOpenLesson = async (lessonId, classId) => {
    if (!lessonId || !classId) {
      setErrorMessage("ID урока или класса не указан.");
      console.error("handleOpenLesson: missing parameters", { lessonId, classId });
      return;
    }

    const targetLesson = lessons.find((lesson) => lesson.id === lessonId);
    if (!targetLesson) {
      setErrorMessage("Урок не найден.");
      console.error("handleOpenLesson: lesson not found", { lessonId, classId });
      return;
    }

    const targetClass = classes.find((classItem) => classItem.id === classId);
    if (!targetClass) {
      setErrorMessage("Класс не найден.");
      console.error("handleOpenLesson: class not found", { lessonId, classId });
      return;
    }

    if (targetLesson.courseId && targetClass.courseId !== targetLesson.courseId) {
      setErrorMessage("Класс не относится к курсу выбранного урока.");
      return;
    }

    try {
      setErrorMessage("");
      const classIdNum = Number(classId);
      const lessonIdNum = Number(lessonId);
      const accessKey = getLessonAccessKey(lessonIdNum, classIdNum);

      if (openedLessonAccesses[accessKey] || openingLessonAccesses[accessKey]) {
        return;
      }

      if (isNaN(classIdNum) || isNaN(lessonIdNum)) {
        setErrorMessage("Некорректные параметры запроса.");
        console.error("Invalid parameters", { classId, lessonId, classIdNum, lessonIdNum });
        return;
      }

      setOpeningLessonAccesses((prev) => ({ ...prev, [accessKey]: true }));
      console.log("Opening lesson for class", { classId: classIdNum, lessonId: lessonIdNum });
      await openLessonForClass(classIdNum, lessonIdNum);
      console.log("Lesson opened successfully", { classId: classIdNum, lessonId: lessonIdNum });
      const openClassIds = await listOpenClassIdsForLesson(lessonIdNum);
      setOpenedLessonAccesses((prev) => {
        const next = { ...prev };
        // Remove stale flags for this lesson, then set the backend-actual state.
        Object.keys(next).forEach((key) => {
          if (key.startsWith(`${lessonIdNum}-`)) {
            delete next[key];
          }
        });
        openClassIds.forEach((openedClassId) => {
          next[getLessonAccessKey(lessonIdNum, openedClassId)] = true;
        });
        return next;
      });
    } catch (error) {
      console.error("Error opening lesson for class", { error, classId, lessonId, status: error?.status, message: error?.message });
      setErrorMessage(error?.message || `Не удалось открыть урок для класса. ${error?.status ? `Статус: ${error.status}` : ""}`);
    } finally {
      const classIdNum = Number(classId);
      const lessonIdNum = Number(lessonId);
      if (!isNaN(classIdNum) && !isNaN(lessonIdNum)) {
        const accessKey = getLessonAccessKey(lessonIdNum, classIdNum);
        setOpeningLessonAccesses((prev) => {
          if (!prev[accessKey]) {
            return prev;
          }
          const next = { ...prev };
          delete next[accessKey];
          return next;
        });
      }
    }
  };

  const handleToggleAccess = async (activityId, classId) => {
    const targetActivity = activities.find((activity) => activity.id === activityId);
    if (targetActivity?.openClasses?.includes(classId)) {
      return;
    }

    // Проверка параметров
    if (!activityId || activityId === null || activityId === undefined) {
      setErrorMessage("ID активности не указан.");
      console.error("handleToggleAccess: activityId is missing", { activityId, classId });
      return;
    }
    if (!classId || classId === null || classId === undefined) {
      setErrorMessage("ID класса не указан.");
      console.error("handleToggleAccess: classId is missing", { activityId, classId });
      return;
    }

    if (!targetActivity) {
      setErrorMessage("Активность не найдена.");
      console.error("handleToggleAccess: activity not found", { activityId, classId });
      return;
    }
    if (targetActivity.status !== "READY") {
      setErrorMessage("Открыть доступ можно только для активности в статусе READY.");
      return;
    }

    const targetClass = classes.find((classItem) => classItem.id === classId);
    if (!targetClass) {
      setErrorMessage("Класс не найден.");
      console.error("handleToggleAccess: class not found", { activityId, classId });
      return;
    }
    if (targetActivity.courseId && targetClass.courseId !== targetActivity.courseId) {
      setErrorMessage("Класс не относится к курсу выбранной активности.");
      return;
    }

    try {
      setErrorMessage("");
      // Убеждаемся, что параметры - числа
      const classIdNum = Number(classId);
      const activityIdNum = Number(activityId);

      if (isNaN(classIdNum) || isNaN(activityIdNum)) {
        setErrorMessage("Некорректные параметры запроса.");
        console.error("Invalid parameters", { classId, activityId, classIdNum, activityIdNum });
        return;
      }

      console.log("Opening activity for class", { classId: classIdNum, activityId: activityIdNum });
      await openActivityForClass(classIdNum, activityIdNum);
      console.log("Activity opened successfully", { classId: classIdNum, activityId: activityIdNum });
      const openClassIds = await listOpenClassIdsForActivity(activityIdNum);
      setActivities((prev) =>
          prev.map((activity) =>
              activity.id !== activityIdNum
                  ? activity
                  : {
                    ...activity,
                    openClasses: openClassIds,
                  },
          ),
      );
    } catch (error) {
      console.error("Error opening activity for class", { error, classId, activityId, status: error?.status, message: error?.message });
      setErrorMessage(error?.message || `Не удалось открыть доступ к активности. ${error?.status ? `Статус: ${error.status}` : ""}`);
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
          </div>

          <section className="test-access-list-section">
            <div className="test-access-list-header">
              <h2 className="test-access-list-title">Уроки</h2>
              <p className="test-access-list-subtitle">
                Откройте урок для класса, чтобы студенты могли видеть его активности
              </p>
            </div>
            <div className="test-access-list">
              {!isLoading && lessons.length === 0 && (
                  <div className="activity-empty">Нет доступных уроков</div>
              )}
              {lessons.map((lesson) => {
                const relevantClasses = classes.filter((classItem) =>
                    lesson.courseId ? classItem.courseId === lesson.courseId : true,
                );
                return (
                    <div key={lesson.id} className="test-access-card">
                      <div className="test-access-card-content">
                        <div className="test-access-card-header">
                          <h3 className="test-access-card-title">
                            {lesson.title || "Без названия"}
                          </h3>
                        </div>
                        <div className="test-access-card-details">
                          <div className="test-access-detail-item">
                            <LocationIcon />
                            <span>Порядок: {lesson.orderIndex || 0}</span>
                          </div>
                          {lesson.description && (
                              <div className="test-access-detail-item">
                                <DocumentIcon />
                                <span>{lesson.description}</span>
                              </div>
                          )}
                        </div>
                      </div>
                      <div className="test-access-class-actions">
                        {relevantClasses.map((classItem) => {
                          const accessKey = getLessonAccessKey(lesson.id, classItem.id);
                          const isOpenForClass = Boolean(openedLessonAccesses[accessKey]);
                          const isOpeningForClass = Boolean(openingLessonAccesses[accessKey]);
                          return (
                              <button
                                  key={classItem.id}
                                  className={`btn-toggle-access ${isOpenForClass ? "opened" : "open"}`}
                                  onClick={() => void handleOpenLesson(lesson.id, classItem.id)}
                                  type="button"
                                  disabled={isOpenForClass || isOpeningForClass}
                              >
                                <OpenLockIcon />
                                {isOpenForClass
                                    ? `Открыто для ${classItem.name}`
                                    : isOpeningForClass
                                        ? `Открывается для ${classItem.name}...`
                                        : `Открыть для ${classItem.name}`}
                              </button>
                          );
                        })}
                      </div>
                    </div>
                );
              })}
            </div>
          </section>

          <section className="test-access-list-section">
            <div className="test-access-list-header">
              <h2 className="test-access-list-title">Активности</h2>
              <p className="test-access-list-subtitle">
                Откройте доступ к активности для нужного класса
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
                            <span>
                          Статус: {activity.status === "READY" ? "Опубликован" : activity.status === "DRAFT" ? "Черновик" : activity.status || "неизвестно"}
                        </span>
                          </div>
                        </div>
                      </div>
                      <div className="test-access-class-actions">
                        {classes
                            .filter((classItem) =>
                                activity.courseId ? classItem.courseId === activity.courseId : true,
                            )
                            .map((classItem) => {
                              const isOpenForClass = Array.isArray(activity.openClasses)
                                  && activity.openClasses.includes(classItem.id);
                              return (
                                  <button
                                      key={classItem.id}
                                      className={`btn-toggle-access ${isOpenForClass ? "opened" : "open"}`}
                                      onClick={() => void handleToggleAccess(activity.id, classItem.id)}
                                      type="button"
                                      disabled={isOpenForClass}
                                  >
                                    {isOpenForClass ? <OpenLockIcon /> : <ManageIcon />}
                                    {isOpenForClass ? `Открыто для ${classItem.name}` : `Открыть для ${classItem.name}`}
                                  </button>
                              );
                            })}
                      </div>
                    </div>
                );
              })}
            </div>
          </section>
        </div>
      </div>
  );
}

export default TestAccess;
