import { useEffect, useMemo, useState } from "react";
import { HomeIcon, BookIcon, MaterialsIcon } from "../../../svgs/MethodistSvg";
import { DownloadIcon, FileIcon } from "../../../svgs/StudyMaterialSvg";
import "./StudyMaterials.css";
import { listMyCourses, listMyLessonsInCourse } from "../../api/studentApi";

function StudyMaterials({ onBackToMain }) {
  const [selectedCourseId, setSelectedCourseId] = useState("");
  const [selectedCourseName, setSelectedCourseName] = useState("");
  const [courses, setCourses] = useState([]);
  const [lessons, setLessons] = useState([]);
  const [isCoursesLoading, setIsCoursesLoading] = useState(true);
  const [isLessonsLoading, setIsLessonsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let isCancelled = false;

    const load = async () => {
      setIsCoursesLoading(true);
      setErrorMessage("");
      try {
        const coursesData = await listMyCourses();
        if (!isCancelled) {
          const rows = Array.isArray(coursesData) ? coursesData : [];
          setCourses(rows);
          setSelectedCourseId((prev) => {
            if (prev && rows.some((c) => String(c.id) === String(prev))) {
              return prev;
            }
            return rows[0]?.id ? String(rows[0].id) : "";
          });
          setSelectedCourseName(rows[0]?.name || "");
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось загрузить курсы");
        }
      } finally {
        if (!isCancelled) {
          setIsCoursesLoading(false);
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
      setLessons([]);
      return;
    }

    let isCancelled = false;

    const loadLessons = async () => {
      setIsLessonsLoading(true);
      setErrorMessage("");
      try {
        const data = await listMyLessonsInCourse(Number(selectedCourseId));
        if (!isCancelled) {
          const normalized = (Array.isArray(data) ? data : []).map((lesson) => ({
            id: lesson?.id ?? lesson?.lessonId ?? null,
            orderIndex: lesson?.orderIndex ?? lesson?.order ?? null,
            title: lesson?.title || lesson?.name || lesson?.lessonTitle || "",
            description:
                lesson?.description || lesson?.content || lesson?.lessonDescription || "",
            presentationUrl:
                lesson?.presentationUrl || lesson?.fileUrl || lesson?.materialUrl || "",
          }));
          setLessons(normalized);
        }
      } catch (e) {
        if (!isCancelled) {
          const errorMsg = e?.message || "Не удалось загрузить материалы курса";
          setErrorMessage(errorMsg);
          setLessons([]);
        }
      } finally {
        if (!isCancelled) {
          setIsLessonsLoading(false);
        }
      }
    };

    loadLessons();
    return () => {
      isCancelled = true;
    };
  }, [selectedCourseId]);

  const selectedCourse = useMemo(
      () => courses.find((course) => String(course.id) === String(selectedCourseId)) || null,
      [courses, selectedCourseId]
  );

  const materialsCount = useMemo(
      () => lessons.filter((lesson) => Boolean(lesson?.presentationUrl)).length,
      [lessons]
  );

  const displayedLessons = useMemo(
      () =>
          lessons.filter(
              (lesson) =>
                  Boolean(lesson?.presentationUrl) ||
                  Boolean(String(lesson?.title || "").trim()) ||
                  Boolean(String(lesson?.description || "").trim())
          ),
      [lessons]
  );

  const getMaterialFileName = (url) => {
    if (!url) return "Материал";
    try {
      const pathname = new URL(url).pathname;
      const decoded = decodeURIComponent(pathname.split("/").pop() || "");
      return decoded || "Материал";
    } catch {
      const fallback = String(url).split("/").pop();
      return fallback || "Материал";
    }
  };

  return (
      <div className="study-materials-management">
        <div className="study-materials-container">
          <header className="study-materials-header">
            <div className="study-materials-header-left">
              <div>
                <h1 className="study-materials-title">Учебные материалы</h1>
                <p className="study-materials-subtitle">
                  Просмотр учебных материалов, опубликованных методистом
                </p>
              </div>
            </div>
            <div className="study-materials-header-right">
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
              <div className="study-materials-empty">
                {errorMessage.includes("fetch") || errorMessage.includes("Failed")
                    ? "Данные временно недоступны."
                    : errorMessage}
              </div>
          )}

          <div className="study-materials-card">
            <h3>Выберите курс</h3>
            <p className="study-materials-muted">
              Выберите курс для просмотра учебных материалов
            </p>
            {isCoursesLoading ? (
                <div className="study-materials-empty">Загрузка...</div>
            ) : (
                <div className="study-materials-course-list">
                  {courses.length > 0 ? (
                      courses.map((course) => (
                          <button
                              key={course.id}
                              className={`study-materials-course-item ${
                                  String(selectedCourseId) === String(course.id) ? "active" : ""
                              }`}
                              onClick={() => {
                                setSelectedCourseId(String(course.id));
                                setSelectedCourseName(course.name || `Курс #${course.id}`);
                              }}
                              type="button"
                          >
                            <div className="study-materials-course-info">
                              <h4>
                                <BookIcon />
                                {course.name || `Курс #${course.id}`}
                              </h4>
                              <p>{course.description || "Без описания"}</p>
                            </div>
                          </button>
                      ))
                  ) : (
                      <div className="study-materials-empty">
                        Вы пока не записаны ни на один курс
                      </div>
                  )}
                </div>
            )}
          </div>

          {selectedCourseId && isLessonsLoading && (
              <div className="study-materials-empty">Загрузка материалов курса...</div>
          )}

          {selectedCourseId && !isLessonsLoading ? (
              <div className="study-materials-list-section">
                <div className="study-materials-list-header">
                  <h2 className="study-materials-list-title">
                    Материалы курса: {selectedCourse?.name || selectedCourseName || "-"}
                  </h2>
                  <p className="study-materials-list-subtitle">
                    Открытые уроки и файлы презентаций
                  </p>
                </div>
                <div className="study-materials-stats">
                  <div className="study-materials-stat-card">
                    <span>Открытых уроков</span>
                    <strong>{displayedLessons.length}</strong>
                  </div>
                  <div className="study-materials-stat-card">
                    <span>Материалов для скачивания</span>
                    <strong>{materialsCount}</strong>
                  </div>
                </div>
                {displayedLessons.length > 0 ? (
                    <div className="study-materials-list">
                      {displayedLessons.map((lesson, idx) => (
                          <div key={lesson.id} className="study-material-card">
                            <div className="study-material-info">
                              <h3 className="study-material-title">
                                <MaterialsIcon />
                                {lesson.title || `Урок ${lesson.orderIndex || idx + 1}`}
                              </h3>
                              <p className="study-material-description">
                                {lesson.description || "Без описания"}
                              </p>
                              {lesson.presentationUrl && (
                                  <div className="study-material-file">
                                    <FileIcon />
                                    <span>{getMaterialFileName(lesson.presentationUrl)}</span>
                                    <div className="study-material-actions">
                                      <a
                                          href={lesson.presentationUrl}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          className="study-material-download"
                                      >
                                        Открыть
                                      </a>
                                      <a
                                          href={lesson.presentationUrl}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          download
                                          className="study-material-download"
                                      >
                                        <DownloadIcon />
                                        Скачать
                                      </a>
                                    </div>
                                  </div>
                              )}
                              {!lesson.presentationUrl && (
                                  <div className="study-material-file">
                                    <FileIcon />
                                    <span>Файл презентации пока не прикреплен</span>
                                  </div>
                              )}
                            </div>
                          </div>
                      ))}
                    </div>
                ) : (
                    <div className="study-materials-empty">
                      В этом курсе пока нет открытых уроков с материалами
                    </div>
                )}
              </div>
          ) : null}
        </div>
      </div>
  );
}

export default StudyMaterials;
