import { useState } from "react";
import { HomeIcon, ClassesIcon, AchievementsIcon } from "../../../svgs/MethodistSvg";
import "./ClassFeed.css";
import { getClassAchievementFeed } from "../../api/studentApi";

function getInitials(name) {
  const value = String(name || "").trim();
  if (!value) return "??";
  const parts = value.split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0] || ""}${parts[1][0] || ""}`.toUpperCase();
}

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

function ClassFeed({ onBackToMain }) {
  const [classId, setClassId] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [feed, setFeed] = useState([]);
  const [hasLoaded, setHasLoaded] = useState(false);

  const handleLoadFeed = async () => {
    const normalizedClassId = String(classId || "").trim();
    if (!normalizedClassId) {
      setErrorMessage("Введите ID класса");
      return;
    }

    const classIdNum = parseInt(normalizedClassId, 10);
    if (isNaN(classIdNum) || classIdNum <= 0) {
      setErrorMessage("ID класса должен быть положительным числом");
      return;
    }

    setIsLoading(true);
    setErrorMessage("");
    setHasLoaded(false);
    setFeed([]);
    try {
      const response = await getClassAchievementFeed(classIdNum, 0, 100);
      const feedData = Array.isArray(response?.content) ? response.content : [];
      setFeed(feedData);
      setHasLoaded(true);
      if (feedData.length === 0) {
        setErrorMessage("В этом классе пока нет достижений");
      }
    } catch (e) {
      const errorMsg = e?.message || "Не удалось загрузить ленту достижений";
      setErrorMessage(errorMsg);
      setFeed([]);
      setHasLoaded(true);
    } finally {
      setIsLoading(false);
    }
  };

  const uniqueStudents = new Set(
    feed.map((item) => item.studentId).filter(Boolean)
  ).size;
  const latestDate = feed.length > 0 ? feed[0]?.awardedAt : null;

  return (
    <div className="class-feed-management">
      <div className="class-feed-container">
        <header className="class-feed-header">
          <div className="class-feed-header-left">
            <div>
              <h1 className="class-feed-title">Лента достижений класса</h1>
              <p className="class-feed-subtitle">
                Все достижения, полученные в процессе обучения каждого ученика в
                классе
              </p>
            </div>
          </div>
          <div className="class-feed-header-right">
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

        <div className="class-feed-card">
          <h3>Фильтр по классу</h3>
          <p className="class-feed-muted">
            Введите ID вашего класса, чтобы просмотреть достижения всех учеников
            этого класса.
          </p>

          <div className="class-feed-inline-form">
            <input
              className="class-feed-input"
              type="number"
              min="1"
              placeholder="ID класса"
              value={classId}
              onChange={(e) => setClassId(e.target.value)}
            />
            <button
              type="button"
              className="class-feed-btn class-feed-btn-primary"
              onClick={handleLoadFeed}
              disabled={isLoading}
            >
              {isLoading ? "Загрузка..." : "Загрузить ленту"}
            </button>
          </div>

          {errorMessage ? (
            <div className="class-feed-empty">
              {errorMessage.includes("fetch") || errorMessage.includes("Failed")
                ? "Данные временно недоступны."
                : errorMessage}
            </div>
          ) : null}
        </div>

        {hasLoaded ? (
          <>
          <div className="class-feed-stats">
            <div className="class-feed-stat-card">
              <span className="class-feed-stat-label">Всего достижений</span>
              <strong>{feed.length}</strong>
            </div>
            <div className="class-feed-stat-card">
              <span className="class-feed-stat-label">Учеников в ленте</span>
              <strong>{uniqueStudents}</strong>
            </div>
            <div className="class-feed-stat-card">
              <span className="class-feed-stat-label">
                Последнее обновление
              </span>
              <strong>{latestDate ? formatDateTime(latestDate) : "—"}</strong>
            </div>
          </div>

          <div className="class-feed-list-card">
            <h3>Лента достижений</h3>
            <p className="class-feed-muted">Последние успехи учеников класса</p>
            {feed.length > 0 ? (
              <div className="class-feed-list">
                {feed.map((item) => (
                  <article className="class-feed-item" key={item.id || Math.random()}>
                    <div className="class-feed-achievement-photo">
                      {item.achievementPhotoUrl ? (
                        <img src={item.achievementPhotoUrl} alt="achievement" />
                      ) : (
                        <AchievementsIcon />
                      )}
                    </div>
                    <div className="class-feed-content">
                      <div className="achievement-info">
                        <strong>
                          <AchievementsIcon />
                          {item.achievementTitle || "Достижение"}
                        </strong>
                        <p>
                          {item.achievementJokeDescription ||
                            item.achievementDescription ||
                            "Без описания"}
                        </p>
                        <div className="class-feed-meta">
                          <span className="class-feed-student-avatar">
                            {getInitials(item.studentName)}
                          </span>
                          <span>{item.studentName || "Ученик"}</span>
                          <span>•</span>
                          <span>{formatDateTime(item.awardedAt)}</span>
                        </div>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            ) : hasLoaded ? (
              <div className="class-feed-empty">
                В этом классе пока нет достижений
              </div>
            ) : (
              <div className="class-feed-list">
                <article className="class-feed-item class-feed-example">
                  <div className="class-feed-achievement-photo">
                    <AchievementsIcon />
                  </div>
                  <div className="class-feed-content">
                    <div className="achievement-info">
                      <strong>
                        <AchievementsIcon />
                        Лучший студент месяца
                      </strong>
                      <p>
                        Отличная работа! Вы показали выдающиеся результаты в
                        обучении.
                      </p>
                      <div className="class-feed-meta">
                        <span className="class-feed-student-avatar">ИП</span>
                        <span>Иван Петров</span>
                        <span>•</span>
                        <span>15 января 2024, 14:30</span>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            )}
          </div>
        </>
      ) : (
        <div className="class-feed-empty">
          Загрузите ленту, чтобы увидеть достижения класса.
        </div>
      )}
      </div>
    </div>
  );
}

export default ClassFeed;
