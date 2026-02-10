import { useState } from "react";
import "./Notifications.css";
import { NotificationsIcon, HomeIcon } from "../../../svgs/TeacherSvg.jsx";
import { CalendarIcon } from "../../../svgs/ActivitySvg.jsx";

function Notifications({ onBackToMain }) {
  const [notifications] = useState([
    {
      id: 1,
      title: "Новое домашнее задание",
      description:
        "Студент Иван Петров сдал домашнее задание по алгебре. Требуется проверка.",
      date: "15.01.2024, 14:30",
      type: "homework",
      isRead: false,
    },
    {
      id: 2,
      title: "Запрос на доступ к тесту",
      description:
        "Студент Мария Сидорова запросила доступ к контрольной работе по геометрии.",
      date: "15.01.2024, 12:15",
      type: "test",
      isRead: false,
    },
    {
      id: 3,
      title: "Новый ответ в обсуждении",
      description:
        "Студент Алексей Иванов оставил комментарий в обсуждении темы 'Квадратные уравнения'.",
      date: "14.01.2024, 18:45",
      type: "discussion",
      isRead: true,
    },
    {
      id: 4,
      title: "Напоминание о проверке",
      description:
        "У вас есть 5 непроверенных домашних заданий, срок проверки истекает завтра.",
      date: "14.01.2024, 10:00",
      type: "reminder",
      isRead: true,
    },
    {
      id: 5,
      title: "Новое сообщение от студента",
      description:
        "Студент Елена Козлова отправила вам сообщение с вопросом по домашнему заданию.",
      date: "13.01.2024, 16:20",
      type: "message",
      isRead: true,
    },
    {
      id: 6,
      title: "Обновление расписания",
      description:
        "Расписание занятий на следующую неделю было обновлено. Проверьте изменения.",
      date: "13.01.2024, 09:00",
      type: "schedule",
      isRead: true,
    },
  ]);

  const unreadCount = notifications.filter((n) => !n.isRead).length;
  const totalNotifications = notifications.length;

  const getNotificationIcon = (type) => {
    switch (type) {
      case "homework":
        return "📝";
      case "test":
        return "📋";
      case "discussion":
        return "💬";
      case "reminder":
        return "⏰";
      case "message":
        return "✉️";
      case "schedule":
        return "📅";
      default:
        return "🔔";
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
            {notifications.map((notification) => (
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
                    {notification.description}
                  </p>
                  <div className="notification-date">
                    <CalendarIcon />
                    <span>{notification.date}</span>
                  </div>
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
