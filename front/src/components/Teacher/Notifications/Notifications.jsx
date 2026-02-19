import { useEffect, useState } from "react";
import "./Notifications.css";
import { NotificationsIcon, HomeIcon } from "../../../svgs/TeacherSvg.jsx";
import { CalendarIcon } from "../../../svgs/ActivitySvg.jsx";
import {
  getMyUnreadNotificationsCount,
  listMyNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "../../api/teacherApi";

function Notifications({ onBackToMain }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isMarkingAll, setIsMarkingAll] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const totalNotifications = notifications.length;

  const loadNotifications = async () => {
    setIsLoading(true);
    setErrorMessage("");
    try {
      const [pageData, unread] = await Promise.all([
        listMyNotifications({ page: 0, size: 100 }),
        getMyUnreadNotificationsCount(),
      ]);
      setNotifications(pageData.content || []);
      setUnreadCount(unread || 0);
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось загрузить уведомления");
      setNotifications([]);
      setUnreadCount(0);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const getNotificationIcon = (type) => {
    switch (type) {
      case "MANUAL_GRADING_REQUIRED":
        return "📝";
      case "CLASS_JOIN_REQUEST":
        return "📨";
      case "GRADE_RECEIVED":
        return "✅";
      case "OPEN_ANSWER_CHECKED":
        return "📋";
      case "WEEKLY_ASSIGNMENT_AVAILABLE":
        return "📅";
      case "ACHIEVEMENT_AWARDED":
        return "🏆";
      default:
        return "🔔";
    }
  };

  const formatDate = (dateValue) => {
    if (!dateValue) return "";
    const parsed = new Date(dateValue);
    if (Number.isNaN(parsed.getTime())) {
      return String(dateValue);
    }
    return parsed.toLocaleString("ru-RU");
  };

  const handleMarkRead = async (notificationId) => {
    try {
      await markNotificationRead(notificationId);
      setNotifications((prev) =>
          prev.map((n) => (n.id === notificationId ? { ...n, isRead: true } : n)),
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось отметить уведомление как прочитанное");
    }
  };

  const handleMarkAllRead = async () => {
    setIsMarkingAll(true);
    setErrorMessage("");
    try {
      await markAllNotificationsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось отметить все уведомления как прочитанные");
    } finally {
      setIsMarkingAll(false);
    }
  };

  return (
      <div className="notifications-management">
        <div className="notifications-container">
          <header className="notifications-header">
            <div className="notifications-header-left">
              <div className="notifications-header-icon">
                <NotificationsIcon />
              </div>
              <div>
                <h1 className="notifications-title">Уведомления</h1>
                <p className="notifications-subtitle">
                  Просмотр всех уведомлений и важных событий
                </p>
              </div>
            </div>
            <div className="notifications-header-actions">
              <button
                  className="btn-secondary"
                  onClick={loadNotifications}
                  type="button"
                  disabled={isLoading}
              >
                Обновить
              </button>
              <button
                  className="btn-secondary"
                  onClick={handleMarkAllRead}
                  type="button"
                  disabled={isLoading || isMarkingAll || unreadCount === 0}
              >
                {isMarkingAll ? "Отмечаем..." : "Прочитать все"}
              </button>
              <button className="btn-home" onClick={onBackToMain} type="button">
                <HomeIcon />
                На главную
              </button>
            </div>
          </header>

          <div className="notifications-stats">
            <div className="stat-card stat-orange">
              <div className="stat-icon">
                <NotificationsIcon />
              </div>
              <div className="stat-content">
                <div className="stat-label">Всего уведомлений</div>
                <div className="stat-value">{totalNotifications}</div>
              </div>
            </div>
            <div className="stat-card stat-orange-light">
              <div className="stat-icon">
                <NotificationsIcon />
              </div>
              <div className="stat-content">
                <div className="stat-label">Непрочитанных</div>
                <div className="stat-value">{unreadCount}</div>
              </div>
            </div>
          </div>

          <section className="notifications-list-section">
            <div className="notifications-list-header">
              <h2 className="notifications-list-title">Список уведомлений</h2>
              <p className="notifications-list-subtitle">
                Все уведомления в хронологическом порядке
              </p>
            </div>
            <div className="notifications-list">
              {errorMessage && (
                  <div className="notifications-empty notifications-error">{errorMessage}</div>
              )}
              {isLoading && !errorMessage && (
                  <div className="notifications-empty">Загрузка уведомлений...</div>
              )}
              {!isLoading && !errorMessage && notifications.length === 0 && (
                  <div className="notifications-empty">Уведомлений пока нет</div>
              )}
              {!isLoading &&
                  !errorMessage &&
                  notifications.map((notification) => (
                      <div
                          key={notification.id}
                          className={`notification-card ${!notification.isRead ? "notification-unread" : ""}`}
                      >
                        <div className="notification-icon">
                    <span className="notification-icon-emoji">
                      {getNotificationIcon(notification.type)}
                    </span>
                        </div>
                        <div className="notification-info">
                          <div className="notification-header">
                            <h3 className="notification-title">{notification.title}</h3>
                            {!notification.isRead && (
                                <span className="notification-badge">Новое</span>
                            )}
                          </div>
                          <p className="notification-description">
                            {notification.message || "Без дополнительного описания"}
                          </p>
                          <div className="notification-date">
                            <CalendarIcon />
                            <span>{formatDate(notification.createdAt)}</span>
                          </div>
                          {!notification.isRead && (
                              <div className="notification-actions">
                                <button
                                    className="btn-mark-read"
                                    type="button"
                                    onClick={() => handleMarkRead(notification.id)}
                                >
                                  Отметить прочитанным
                                </button>
                              </div>
                          )}
                        </div>
                      </div>
                  ))}
            </div>
          </section>
        </div>
      </div>
  );
}

export default Notifications;
