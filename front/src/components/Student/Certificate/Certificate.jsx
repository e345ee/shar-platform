import { useEffect, useMemo, useState } from "react";
import { HomeIcon, BookIcon, AchievementsIcon } from "../../../svgs/MethodistSvg";
import "./Certificate.css";
import {
  getMyCoursePage,
  getMyStatisticsOverview,
  listMyCourses,
  sendCompletionEmail,
} from "../../api/studentApi";

function Certificate({ onBackToMain }) {
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [isCheckingCourseClosed, setIsCheckingCourseClosed] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [courseClosedMap, setCourseClosedMap] = useState({});

  const selectedCourse = useMemo(
      () => courses.find((course) => String(course.id) === String(selectedCourseId)) || null,
      [courses, selectedCourseId]
  );

  const completedCoursesCount = useMemo(
      () => courses.filter((course) => course.completed).length,
      [courses]
  );

  const selectedCourseClosed = selectedCourse
      ? Boolean(courseClosedMap[String(selectedCourse.id)])
      : false;

  const loadData = async (isCancelledRef) => {
    setIsLoading(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const [coursesData, overviewData] = await Promise.all([
        listMyCourses(),
        getMyStatisticsOverview(),
      ]);

      const overviewCourses = Array.isArray(overviewData?.courses)
          ? overviewData.courses
          : [];
      const progressMap = new Map(
          overviewCourses.map((item) => [String(item.courseId), item])
      );

      const mergedCourses = (Array.isArray(coursesData) ? coursesData : []).map((course) => {
        const progress = progressMap.get(String(course.id));
        return {
          ...course,
          progress: progress || null,
          completed: Boolean(progress?.completed),
          requiredTests: Number(progress?.requiredTests ?? 0),
          completedTests: Number(progress?.completedTests ?? 0),
          percent: progress?.percent ?? null,
        };
      });

      if (!isCancelledRef.current) {
        setCourses(mergedCourses);
        setCourseClosedMap({});
        setSelectedCourseId((prev) => {
          const prevExists = mergedCourses.some(
              (course) => String(course.id) === String(prev)
          );
          if (prevExists) {
            return prev;
          }
          const firstCompleted = mergedCourses.find((course) => course.completed);
          return firstCompleted ? String(firstCompleted.id) : String(mergedCourses[0]?.id || "");
        });
      }
    } catch (e) {
      if (!isCancelledRef.current) {
        setCourses([]);
        setErrorMessage(e?.message || "Не удалось загрузить данные по сертификатам");
      }
    } finally {
      if (!isCancelledRef.current) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    const isCancelledRef = { current: false };
    loadData(isCancelledRef);
    return () => {
      isCancelledRef.current = true;
    };
  }, []);

  useEffect(() => {
    let isCancelled = false;

    const loadCourseClosed = async () => {
      if (!selectedCourseId) {
        return;
      }
      const key = String(selectedCourseId);
      if (Object.prototype.hasOwnProperty.call(courseClosedMap, key)) {
        return;
      }

      setIsCheckingCourseClosed(true);
      try {
        const page = await getMyCoursePage(Number(selectedCourseId));
        if (!isCancelled) {
          setCourseClosedMap((prev) => ({
            ...prev,
            [key]: Boolean(page?.courseClosed),
          }));
        }
      } catch (e) {
        if (!isCancelled) {
          setCourseClosedMap((prev) => ({
            ...prev,
            [key]: false,
          }));
        }
      } finally {
        if (!isCancelled) {
          setIsCheckingCourseClosed(false);
        }
      }
    };

    loadCourseClosed();
    return () => {
      isCancelled = true;
    };
  }, [selectedCourseId, courseClosedMap]);

  const handleReload = async () => {
    const isCancelledRef = { current: false };
    await loadData(isCancelledRef);
  };

  const handleSendEmail = async () => {
    if (!selectedCourse) {
      setErrorMessage("Выберите курс");
      return;
    }
    if (!selectedCourseClosed) {
      setErrorMessage("Сертификат доступен после закрытия курса преподавателем");
      return;
    }

    setIsSending(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      await sendCompletionEmail(Number(selectedCourse.id));
      setSuccessMessage(
          `Сертификат по курсу "${selectedCourse.name || `Курс #${selectedCourse.id}`}" отправлен на вашу почту`
      );
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось отправить сертификат");
    } finally {
      setIsSending(false);
    }
  };

  return (
      <div className="certificate-management">
        <div className="certificate-container">
          <header className="certificate-header">
            <div className="certificate-header-left">
              <div>
                <h1 className="certificate-title">Сертификаты</h1>
                <p className="certificate-subtitle">
                  Выберите курс и отправьте сертификат на почту после завершения
                </p>
              </div>
            </div>
            <div className="certificate-header-right">
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
              <div className="certificate-error">
                {errorMessage.includes("fetch") || errorMessage.includes("Failed")
                    ? "Данные временно недоступны."
                    : errorMessage}
              </div>
          )}

          {successMessage && (
              <div className="certificate-success">{successMessage}</div>
          )}

          <div className="certificate-stats">
            <div className="certificate-stat-card">
              <span>Курсов доступно</span>
              <strong>{courses.length}</strong>
            </div>
            <div className="certificate-stat-card">
              <span>Можно получить сертификат</span>
              <strong>{Object.values(courseClosedMap).filter(Boolean).length}</strong>
            </div>
          </div>

          <div className="certificate-card">
            {isLoading ? (
                <div className="certificate-empty">Загрузка курсов...</div>
            ) : courses.length === 0 ? (
                <div className="certificate-empty">
                  У вас пока нет курсов для получения сертификата
                </div>
            ) : (
                <>
                  <div className="certificate-form">
                    <label htmlFor="course-select">Курс:</label>
                    <select
                        id="course-select"
                        value={selectedCourseId}
                        onChange={(e) => {
                          setSelectedCourseId(e.target.value);
                          setErrorMessage("");
                          setSuccessMessage("");
                        }}
                        className="certificate-select"
                    >
                      {courses.map((course) => (
                          <option key={course.id} value={course.id}>
                            {(course.completed ? "[Завершен] " : "[В процессе] ") +
                                (course.name || `Курс #${course.id}`)}
                          </option>
                      ))}
                    </select>
                  </div>

                  {selectedCourse ? (
                      <div className="certificate-course-status">
                        <div className="certificate-course-status-title">
                          <BookIcon />
                          <span>{selectedCourse.name || `Курс #${selectedCourse.id}`}</span>
                        </div>
                        <div className="certificate-course-status-meta">
                    <span>
                      Прогресс:{" "}
                      {selectedCourse.percent === null || selectedCourse.percent === undefined
                          ? "—"
                          : `${Math.round(selectedCourse.percent)}%`}
                    </span>
                          <span>
                      Тесты: {selectedCourse.completedTests}/{selectedCourse.requiredTests}
                    </span>
                          <span
                              className={
                                selectedCourseClosed
                                    ? "certificate-badge completed"
                                    : "certificate-badge in-progress"
                              }
                          >
                      {isCheckingCourseClosed
                          ? "Проверяем статус..."
                          : selectedCourseClosed
                              ? "Курс закрыт преподавателем"
                              : "Курс еще не закрыт преподавателем"}
                    </span>
                        </div>
                      </div>
                  ) : null}

                  <div className="certificate-actions">
                    <button
                        className="certificate-btn certificate-btn-secondary"
                        onClick={handleReload}
                        disabled={isLoading || isSending}
                        type="button"
                    >
                      Обновить
                    </button>
                    <button
                        className="certificate-btn certificate-btn-primary"
                        onClick={handleSendEmail}
                        disabled={!selectedCourse || !selectedCourseClosed || isSending || isCheckingCourseClosed}
                        type="button"
                    >
                      {isSending
                          ? "Отправка..."
                          : "Отправить сертификат на почту"}
                    </button>
                  </div>
                </>
            )}
          </div>

          <div className="certificate-info-card">
            <h3>
              <AchievementsIcon />
              Как это работает
            </h3>
            <ul>
              <li>Сертификат отправляется только по завершенному курсу</li>
            </ul>
          </div>
        </div>
      </div>
  );
}

export default Certificate;
