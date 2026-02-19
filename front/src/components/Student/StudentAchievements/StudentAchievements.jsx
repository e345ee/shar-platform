import { useEffect, useMemo, useState } from "react";
import { HomeIcon, AchievementsIcon } from "../../../svgs/MethodistSvg";
import "./StudentAchievements.css";
import { getMyAchievementsPage, listMyCourses } from "../../api/studentApi";

function formatDateTime(value) {
  if (!value) return "—";
  try {
    const date = new Date(value);
    return new Intl.DateTimeFormat("ru-RU", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }).format(date);
  } catch {
    return String(value);
  }
}

function StudentAchievements({ onBackToMain }) {
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [achievementsPage, setAchievementsPage] = useState(null);
  const [coursesById, setCoursesById] = useState({});
  const [activeTab, setActiveTab] = useState("earned");

  useEffect(() => {
    let isCancelled = false;

    const load = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const [page, courses] = await Promise.all([
          getMyAchievementsPage(),
          listMyCourses(),
        ]);
        if (!isCancelled) {
          setAchievementsPage(page);
          const mapped = {};
          (Array.isArray(courses) ? courses : []).forEach((course) => {
            if (course?.id != null) {
              mapped[course.id] = course.name || `Курс #${course.id}`;
            }
          });
          setCoursesById(mapped);
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось загрузить достижения");
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

  const earned = useMemo(
      () => (Array.isArray(achievementsPage?.earned) ? achievementsPage.earned : []),
      [achievementsPage],
  );
  const recommendations = useMemo(
      () =>
          Array.isArray(achievementsPage?.recommendations)
              ? achievementsPage.recommendations
              : [],
      [achievementsPage],
  );
  const totalEarned = Number(achievementsPage?.totalEarned ?? earned.length ?? 0);
  const totalAvailable = Number(achievementsPage?.totalAvailable ?? 0);
  const progressPercent =
      totalAvailable > 0 ? Math.min(100, Math.round((totalEarned / totalAvailable) * 100)) : 0;

  const currentList = activeTab === "earned" ? earned : recommendations;
  const isEarnedTab = activeTab === "earned";

  return (
      <div className="personal-achievements-management">
        <div className="personal-achievements-container">
          <header className="personal-achievements-header">
            <div className="personal-achievements-header-left">
              <div>
                <h1 className="personal-achievements-title">Личные достижения</h1>
                <p className="personal-achievements-subtitle">
                  Ваши награды и потенциальные достижения, которые можно получить
                  дальше
                </p>
              </div>
            </div>
            <div className="personal-achievements-header-right">
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

          {isLoading ? (
              <div className="personal-achievements-empty">Загрузка...</div>
          ) : errorMessage ? (
              <div className="personal-achievements-empty">
                {errorMessage.includes("fetch") || errorMessage.includes("Failed")
                    ? "Данные временно недоступны."
                    : errorMessage}
              </div>
          ) : achievementsPage ? (
              <>
                <div className="personal-achievements-stats">
                  <div className="stat-card stat-yellow">
                    <div className="stat-content">
                      <div className="stat-label">Получено</div>
                      <div className="stat-value">
                        {achievementsPage.totalEarned ?? 0}
                      </div>
                    </div>
                  </div>
                  <div className="stat-card stat-blue">
                    <div className="stat-content">
                      <div className="stat-label">Доступно всего</div>
                      <div className="stat-value">
                        {totalAvailable}
                      </div>
                    </div>
                  </div>
                  <div className="stat-card stat-progress">
                    <div className="stat-content">
                      <div className="stat-label">Прогресс коллекции</div>
                      <div className="stat-value">{progressPercent}%</div>
                    </div>
                  </div>
                </div>

                <div className="personal-achievements-list-card">
                  <div className="personal-achievements-card-top">
                    <div>
                      <h3>Достижения</h3>
                      <p>
                        {isEarnedTab
                            ? "Все полученные вами награды"
                            : "Награды, которые можно получить дальше"}
                      </p>
                    </div>
                    <div className="personal-achievements-tabs">
                      <button
                          type="button"
                          className={`achievements-tab ${isEarnedTab ? "active" : ""}`}
                          onClick={() => setActiveTab("earned")}
                      >
                        Полученные ({earned.length})
                      </button>
                      <button
                          type="button"
                          className={`achievements-tab ${!isEarnedTab ? "active" : ""}`}
                          onClick={() => setActiveTab("recommendations")}
                      >
                        Доступные ({recommendations.length})
                      </button>
                    </div>
                  </div>

                  {currentList.length > 0 ? (
                      <div className="personal-achievements-grid">
                        {currentList.map((item) => (
                            <article
                                className="personal-achievement-card"
                                key={`${activeTab}-${item.id}`}
                            >
                              {(isEarnedTab ? item.achievementPhotoUrl : item.photoUrl) ? (
                                  <img
                                      src={isEarnedTab ? item.achievementPhotoUrl : item.photoUrl}
                                      alt={isEarnedTab ? item.achievementTitle || "achievement" : item.title || "achievement"}
                                  />
                              ) : (
                                  <div className="personal-achievement-placeholder">
                                    <AchievementsIcon />
                                  </div>
                              )}
                              <div className="achievement-info">
                                <div className="achievement-title-row">
                                  <strong>
                                    <AchievementsIcon />
                                    {isEarnedTab
                                        ? item.achievementTitle || "Без названия"
                                        : item.title || "Без названия"}
                                  </strong>
                                  {isEarnedTab && (
                                      <span className="achievement-status-badge">Получено</span>
                                  )}
                                </div>
                                <span>
                          {(isEarnedTab
                                  ? item.achievementJokeDescription || item.achievementDescription
                                  : item.jokeDescription || item.description) ||
                              "Без описания"}
                        </span>
                                <small className="achievement-meta-row">
                                  {isEarnedTab ? (
                                      <>
                                        <span>Получено: {formatDateTime(item.awardedAt)}</span>
                                        {item.awardedByName ? <span>Кем выдано: {item.awardedByName}</span> : null}
                                        {item.achievementCourseId != null ? (
                                            <span>
                                  Курс:{" "}
                                              {coursesById[item.achievementCourseId] ||
                                                  `Курс #${item.achievementCourseId}`}
                                </span>
                                        ) : null}
                                      </>
                                  ) : (
                                      item.courseId != null && (
                                          <span>
                                Курс: {coursesById[item.courseId] || `Курс #${item.courseId}`}
                              </span>
                                      )
                                  )}
                                </small>
                              </div>
                            </article>
                        ))}
                      </div>
                  ) : (
                      <div className="personal-achievements-empty">
                        {isEarnedTab
                            ? "У вас пока нет полученных достижений."
                            : "Сейчас нет новых достижений в рекомендациях."}
                      </div>
                  )}
                </div>
              </>
          ) : null}
        </div>
      </div>
  );
}

export default StudentAchievements;
