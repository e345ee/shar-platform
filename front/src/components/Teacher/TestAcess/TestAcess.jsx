import { useState } from "react";
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

function TestAccess({ onBackToMain }) {
  const [showModal, setShowModal] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState(null);

  const [activities] = useState([
    {
      id: 1,
      title: "Контрольная работа по алгебре",
      topic: "Квадратные уравнения",
      format: "Контрольная работа",
      type: "Тест",
      date: "01.02.2024",
      questionsCount: 10,
      openClasses: ["9A"],
      color: "red",
    },
    {
      id: 2,
      title: "Сочинение: Творчество Пушкина",
      topic: "Литература 19 века",
      format: "Домашнее задание",
      type: "Текст",
      date: "28.01.2024",
      questionsCount: 1,
      openClasses: ["9A"],
      color: "blue",
    },
    {
      id: 3,
      title: "Тест по физике: Механика",
      topic: "Законы Ньютона",
      format: "Еженедельное задание",
      type: "Тест",
      date: "25.01.2024",
      questionsCount: 15,
      openClasses: ["10A"],
      color: "green",
    },
    {
      id: 4,
      title: "Домашнее задание: Тригонометрия",
      topic: "Тригонометрические функции",
      format: "Домашнее задание",
      type: "Тест",
      date: "20.01.2024",
      questionsCount: 8,
      openClasses: [],
      color: "blue",
    },
  ]);

  const classes = [
    { id: "9A", name: "9A", subject: "Математика" },
    { id: "9Б", name: "9Б", subject: "Математика" },
    { id: "10A", name: "10A", subject: "Алгебра" },
    { id: "10Б", name: "10Б", subject: "Физика" },
    { id: "11A", name: "11A", subject: "Алгебра" },
  ];

  const stats = {
    totalActivities: activities.length,
    totalClasses: classes.length,
    openAccesses: activities.reduce(
      (sum, activity) => sum + activity.openClasses.length,
      0,
    ),
  };

  const handleOpenModal = (activity) => {
    setSelectedActivity(activity);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedActivity(null);
  };

  const handleToggleAccess = (activityId, classId, isOpen) => {
    // Здесь можно добавить логику обновления доступа
    console.log("Toggle access:", activityId, classId, isOpen);
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
            {activities.map((activity) => {
              const tags = getActivityTypeTag(activity.format, activity.type);
              const isOpen = activity.openClasses.length > 0;
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
                    </div>
                    <div className="test-access-status">
                      {isOpen ? (
                        <>
                          <OpenLockIcon />
                          <span>
                            Открыт для {activity.openClasses.length} класса
                          </span>
                          <span className="test-access-classes">
                            ({activity.openClasses.join(", ")})
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
        classes={classes}
        onToggleAccess={handleToggleAccess}
      />
    </div>
  );
}

export default TestAccess;
