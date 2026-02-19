import { useEffect, useState } from "react";
import { HomeIcon, BookIcon } from "../../../svgs/MethodistSvg";
import "./ActivityResults.css";
import {
  getMyStatisticsOverview,
  getMyCoursePage,
  listMyCourses,
} from "../../api/studentApi";

function normalizeAttemptStatus(status) {
  const value = String(status || "").toUpperCase();
  if (value === "GRADED") return "Проверено";
  if (value === "SUBMITTED") return "На проверке";
  if (value === "IN_PROGRESS") return "В процессе";
  return "Не начато";
}

function calcPercent(score, maxScore) {
  const s = Number(score);
  const m = Number(maxScore);
  if (!Number.isFinite(s) || !Number.isFinite(m) || m <= 0) return null;
  return Math.round((s / m) * 100);
}

function formatDate(value) {
  if (!value) return "—";
  try {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return date.toLocaleString("ru-RU");
  } catch {
    return String(value);
  }
}

function ActivityResults({ onBackToMain }) {
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [overview, setOverview] = useState(null);
  const [courses, setCourses] = useState([]);
  const [rows, setRows] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState("all");

  useEffect(() => {
    let isCancelled = false;

    const collectRowsFromPage = (course, page) => {
      const list = [];
      const seen = new Set();
      const pushItem = (activityWithAttempt) => {
        const activity = activityWithAttempt?.activity;
        if (!activity || activity.id == null) return;
        if (seen.has(activity.id)) return;
        seen.add(activity.id);

        const latest = activityWithAttempt?.latestAttempt || null;
        const percent = calcPercent(latest?.score, latest?.maxScore);
        list.push({
          activityId: activity.id,
          title: activity.title || `Активность #${activity.id}`,
          activityType: activity.activityType || "-",
          courseId: course?.id ?? activity.courseId ?? null,
          courseName: course?.name || `Курс #${activity.courseId || "?"}`,
          deadline: activity.deadline || null,
          attemptStatus: latest?.status || null,
          attemptStatusLabel: normalizeAttemptStatus(latest?.status),
          score: latest?.score ?? null,
          maxScore: latest?.maxScore ?? null,
          weightedScore: latest?.weightedScore ?? null,
          weightedMaxScore: latest?.weightedMaxScore ?? null,
          submittedAt: latest?.submittedAt || null,
          percent,
        });
      };

      const lessons = Array.isArray(page?.lessons) ? page.lessons : [];
      lessons.forEach((lessonBlock) => {
        const items = Array.isArray(lessonBlock?.activities)
            ? lessonBlock.activities
            : [];
        items.forEach(pushItem);
      });
      const weekly = Array.isArray(page?.weeklyThisWeek) ? page.weeklyThisWeek : [];
      weekly.forEach(pushItem);
      const remedial = Array.isArray(page?.remedialThisWeek)
          ? page.remedialThisWeek
          : [];
      remedial.forEach(pushItem);
      return list;
    };

    const load = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const [overviewData, coursesData] = await Promise.all([
          getMyStatisticsOverview(),
          listMyCourses(),
        ]);

        const pageResults = await Promise.allSettled(
            (coursesData || []).map((course) => getMyCoursePage(course.id)),
        );

        const mergedRows = [];
        pageResults.forEach((result, idx) => {
          if (result.status !== "fulfilled") return;
          const course = coursesData[idx];
          mergedRows.push(...collectRowsFromPage(course, result.value));
        });

        if (!isCancelled) {
          setOverview(overviewData);
          setCourses(coursesData);
          setRows(mergedRows);
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось загрузить результаты");
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

  const filteredRows =
      selectedCourseId === "all"
          ? rows
          : rows.filter((r) => String(r.courseId) === String(selectedCourseId));

  const totalActivities = filteredRows.length;
  const completedActivities = filteredRows.filter((r) =>
      ["SUBMITTED", "GRADED"].includes(String(r.attemptStatus || "").toUpperCase()),
  ).length;
  const gradedActivities = filteredRows.filter(
      (r) => String(r.attemptStatus || "").toUpperCase() === "GRADED",
  ).length;
  const percents = filteredRows
      .map((r) => r.percent)
      .filter((v) => Number.isFinite(v));
  const averagePercent = percents.length
      ? Math.round(percents.reduce((a, b) => a + b, 0) / percents.length)
      : 0;

  return (
      <div className="activity-results-management">
        <div className="activity-results-container">
          <header className="activity-results-header">
            <div className="activity-results-header-left">
              <div>
                <h1 className="activity-results-title">Результаты активности</h1>
                <p className="activity-results-subtitle">
                  Автоматическое получение результатов на учебные активности с
                  автоматической проверкой и мгновенной обратной связью
                </p>
              </div>
            </div>
            <div className="activity-results-header-right">
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
              <div className="activity-results-empty">
                {errorMessage.includes("fetch") || errorMessage.includes("Failed")
                    ? "Данные временно недоступны."
                    : errorMessage}
              </div>
          )}

          {isLoading ? (
              <div className="activity-results-empty">Загрузка...</div>
          ) : overview ? (
              <>
                <div className="activity-results-stats">
                  <div className="stat-card stat-blue">
                    <div className="stat-content">
                      <div className="stat-label">Активностей с результатами</div>
                      <div className="stat-value">{totalActivities}</div>
                    </div>
                  </div>
                  <div className="stat-card stat-green">
                    <div className="stat-content">
                      <div className="stat-label">Завершено / сдано</div>
                      <div className="stat-value">{completedActivities}</div>
                    </div>
                  </div>
                  <div className="stat-card stat-yellow">
                    <div className="stat-content">
                      <div className="stat-label">Проверено / средний %</div>
                      <div className="stat-value">
                        {gradedActivities} / {averagePercent}%
                      </div>
                    </div>
                  </div>
                </div>

                <section className="activity-results-list-section">
                  <div className="activity-results-list-header">
                    <h2 className="activity-results-list-title">
                      Результаты по активностям
                    </h2>
                    <p className="activity-results-list-subtitle">
                      Таблица последних результатов по каждой активности
                    </p>
                  </div>
                  <div className="activity-results-filters">
                    <label htmlFor="activity-results-course-filter">Курс:</label>
                    <select
                        id="activity-results-course-filter"
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
                  {filteredRows.length > 0 ? (
                      <div className="activity-results-table-wrap">
                        <table className="activity-results-table">
                          <thead>
                          <tr>
                            <th>Активность</th>
                            <th>Курс</th>
                            <th>Статус</th>
                            <th>Баллы</th>
                            <th>Процент</th>
                            <th>Сдано</th>
                            <th>Дедлайн</th>
                          </tr>
                          </thead>
                          <tbody>
                          {filteredRows.map((item) => (
                              <tr key={item.activityId}>
                                <td>
                                  <div className="activity-results-course-name">
                                    <BookIcon />
                                    <span>{item.title || "-"}</span>
                                  </div>
                                </td>
                                <td>{item.courseName || "-"}</td>
                                <td>
                            <span
                                className={`activity-status-badge status-${String(
                                    item.attemptStatus || "NOT_STARTED",
                                ).toLowerCase()}`}
                            >
                              {item.attemptStatusLabel}
                            </span>
                                </td>
                                <td>
                                  {item.score != null && item.maxScore != null
                                      ? `${item.score}/${item.maxScore}`
                                      : "—"}
                                </td>
                                <td>
                                  {item.percent != null ? `${item.percent}%` : "—"}
                                </td>
                                <td>{formatDate(item.submittedAt)}</td>
                                <td>{formatDate(item.deadline)}</td>
                              </tr>
                          ))}
                          </tbody>
                        </table>
                      </div>
                  ) : (
                      <div className="activity-results-empty">
                        По выбранному фильтру результатов пока нет
                      </div>
                  )}
                </section>

                <section className="activity-results-info-section">
                  <div className="activity-results-info-card">
                    <h3>Что означают статусы</h3>
                    <ul>
                      <li><strong>Не начато</strong> — попытка по активности еще не создавалась.</li>
                      <li><strong>В процессе</strong> — активность начата, но еще не отправлена.</li>
                      <li><strong>На проверке</strong> — отправлена, ожидает проверки преподавателем.</li>
                      <li><strong>Проверено</strong> — итоговый результат уже выставлен.</li>
                    </ul>
                  </div>
                </section>
              </>
          ) : !isLoading && !errorMessage && (
              <div className="activity-results-empty">
                Нет данных для отображения
              </div>
          )}
        </div>
      </div>
  );
}

export default ActivityResults;
