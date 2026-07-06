import { useEffect, useState } from "react";
import { HomeIcon, BookIcon, ActivitiesIcon } from "../../../svgs/MethodistSvg";
import "./Statistics.css";
import {
  getMyStatisticsOverview,
  getMyStatisticsTopics,
  listMyCourses,
} from "../../api/studentApi";

function Statistics({ onBackToMain }) {
  const [isLoading, setIsLoading] = useState(false);
  const [isTopicsLoading, setIsTopicsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [overview, setOverview] = useState(null);
  const [topics, setTopics] = useState([]);
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState("all");

  useEffect(() => {
    let isCancelled = false;

    const load = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const [overviewData, coursesData] = await Promise.all([
          getMyStatisticsOverview(),
          listMyCourses(),
        ]);

        if (!isCancelled) {
          setOverview(overviewData);
          setCourses(coursesData);
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось загрузить статистику");
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
    let isCancelled = false;

    const loadTopics = async () => {
      setIsTopicsLoading(true);
      try {
        const courseId = selectedCourseId === "all" ? null : selectedCourseId;
        const data = await getMyStatisticsTopics(courseId);
        if (!isCancelled) {
          setTopics(Array.isArray(data) ? data : []);
        }
      } catch (e) {
        if (!isCancelled) {
          setTopics([]);
        }
      } finally {
        if (!isCancelled) {
          setIsTopicsLoading(false);
        }
      }
    };

    loadTopics();
    return () => {
      isCancelled = true;
    };
  }, [selectedCourseId]);

  const overviewCourses = Array.isArray(overview?.courses) ? overview.courses : [];

  return (
      <div className="statistics-management">
        <div className="statistics-container">
          <header className="statistics-header">
            <div className="statistics-header-left">
              <div>
                <h1 className="statistics-title">
                  <ActivitiesIcon />
                  Статистика
                </h1>
                <p className="statistics-subtitle">
                  Результаты учебной активности, количество выполненных заданий,
                  достижения и прогресс в обучении
                </p>
              </div>
            </div>
            <div className="statistics-header-right">
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
              <div className="statistics-empty">
                {errorMessage.includes("fetch") || errorMessage.includes("Failed")
                    ? "Данные временно недоступны."
                    : errorMessage}
              </div>
          )}

          {isLoading ? (
              <div className="statistics-empty">Загрузка...</div>
          ) : (
              <>
                <div className="statistics-stats">
                  <div className="stat-card stat-blue">
                    <div className="stat-content">
                      <div className="stat-label">Всего попыток</div>
                      <div className="stat-value">{overview?.attemptsTotal ?? 0}</div>
                    </div>
                  </div>
                  <div className="stat-card stat-green">
                    <div className="stat-content">
                      <div className="stat-label">Завершено попыток</div>
                      <div className="stat-value">{overview?.attemptsFinished ?? 0}</div>
                    </div>
                  </div>
                  <div className="stat-card stat-yellow">
                    <div className="stat-content">
                      <div className="stat-label">Проверено</div>
                      <div className="stat-value">{overview?.attemptsGraded ?? 0}</div>
                    </div>
                  </div>
                  <div className="stat-card stat-purple">
                    <div className="stat-content">
                      <div className="stat-label">Завершено курсов</div>
                      <div className="stat-value">{overview?.coursesCompleted ?? 0}</div>
                    </div>
                  </div>
                </div>

                <section className="statistics-list-section">
                  <div className="statistics-list-header">
                    <h2 className="statistics-list-title">Прогресс по курсам</h2>
                    <p className="statistics-list-subtitle">
                      Соотношение завершенных активностей к обязательным
                    </p>
                  </div>
                  {overviewCourses.length > 0 ? (
                      <div className="statistics-table-wrap">
                        <table className="statistics-table">
                          <thead>
                          <tr>
                            <th>Курс</th>
                            <th>Обязательных тестов</th>
                            <th>Завершено тестов</th>
                            <th>Прогресс</th>
                            <th>Статус курса</th>
                          </tr>
                          </thead>
                          <tbody>
                          {overviewCourses.map((item) => (
                              <tr key={item.courseId}>
                                <td>
                                  <div className="statistics-course-name">
                                    <BookIcon />
                                    <span>{item.courseName || "-"}</span>
                                  </div>
                                </td>
                                <td>{item.requiredTests ?? 0}</td>
                                <td>{item.completedTests ?? 0}</td>
                                <td>
                                  {item.percent !== undefined && item.percent !== null
                                      ? `${Math.round(item.percent)}%`
                                      : "-"}
                                </td>
                                <td>{item.completed ? "Завершен" : "В процессе"}</td>
                              </tr>
                          ))}
                          </tbody>
                        </table>
                      </div>
                  ) : (
                      <div className="statistics-empty">Данных по курсам пока нет</div>
                  )}
                </section>

                <section className="statistics-list-section">
                  <div className="statistics-list-header">
                    <h2 className="statistics-list-title">Результаты по темам</h2>
                    <p className="statistics-list-subtitle">
                      Ваши результаты по темам курсов
                    </p>
                  </div>
                  <div className="statistics-filter">
                    <label htmlFor="course-filter">Фильтр по курсу:</label>
                    <select
                        id="course-filter"
                        value={selectedCourseId}
                        onChange={(e) => setSelectedCourseId(e.target.value)}
                    >
                      <option value="all">Все курсы</option>
                      {courses.map((course) => (
                          <option key={course.id} value={course.id}>
                            {course.name || `Курс #${course.id}`}
                          </option>
                      ))}
                    </select>
                  </div>
                  {isTopicsLoading ? (
                      <div className="statistics-empty">Загрузка тем...</div>
                  ) : topics.length > 0 ? (
                      <div className="statistics-table-wrap">
                        <table className="statistics-table">
                          <thead>
                          <tr>
                            <th>Тема</th>
                            <th>Курс</th>
                            <th>Попыток (всего)</th>
                            <th>Проверено попыток</th>
                            <th>Тестов завершено</th>
                            <th>Средний балл</th>
                          </tr>
                          </thead>
                          <tbody>
                          {topics.map((item, idx) => (
                              <tr key={idx}>
                                <td>{item.topic || "-"}</td>
                                <td>{item.courseName || "-"}</td>
                                <td>{item.attemptsCount ?? 0}</td>
                                <td>{item.gradedAttemptsCount ?? 0}</td>
                                <td>{item.testsAttempted ?? 0}</td>
                                <td>
                                  {item.avgBestPercent !== undefined &&
                                  item.avgBestPercent !== null
                                      ? `${Math.round(item.avgBestPercent)}%`
                                      : "-"}
                                </td>
                              </tr>
                          ))}
                          </tbody>
                        </table>
                      </div>
                  ) : (
                      <div className="statistics-empty">
                        По выбранному фильтру данных по темам пока нет
                      </div>
                  )}
                </section>
              </>
          )}
        </div>
      </div>
  );
}

export default Statistics;
