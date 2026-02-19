import { useState, useEffect } from "react";
import "./TestAcess.css";
import { CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { OpenLockIcon, ClosedLockIcon } from "../../../svgs/TestSvg.jsx";

function TestAccessModal({
  isOpen,
  onClose,
  activity,
  classes,
  onToggleAccess,
}) {
  const [classAccesses, setClassAccesses] = useState({});

  useEffect(() => {
    if (activity && classes) {
      const accesses = {};
      classes.forEach((classItem) => {
        const isOpen = activity.openClasses.includes(classItem.id);
        accesses[classItem.id] = {
          isOpen,
          date: isOpen ? "04.02.2026" : "03.02.2024",
        };
      });
      setClassAccesses(accesses);
    }
  }, [activity, classes]);

  const handleToggle = (classId) => {
    const currentAccess = classAccesses[classId];
    const newIsOpen = !currentAccess.isOpen;
    const newDate = new Date().toLocaleDateString("ru-RU", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });

    setClassAccesses((prev) => ({
      ...prev,
      [classId]: {
        isOpen: newIsOpen,
        date: newDate,
      },
    }));

    if (onToggleAccess) {
      onToggleAccess(activity.id, classId, newIsOpen);
    }
  };

  if (!isOpen || !activity) return null;

  const getActivityTypeTag = (format, type) => {
    const formatColors = {
      "Контрольная работа": "red",
      "Домашнее задание": "blue",
      "Еженедельное задание": "green",
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

  const tags = getActivityTypeTag(activity.format, activity.type);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content modal-content-large"
        onClick={(e) => e.stopPropagation()}
      >
        <button className="modal-close" onClick={onClose} type="button">
          <CloseIcon />
        </button>

        <div className="test-access-modal-header">
          <h2 className="test-access-modal-title">{activity.title}</h2>
          <div className="test-access-modal-meta">
            <span>{activity.topic}</span>
            <span className={`test-access-modal-tag tag-${tags.type.color}`}>
              {activity.type}
            </span>
            <span>{activity.questionsCount} вопросов</span>
          </div>
        </div>

        <div className="test-access-modal-content">
          <h3 className="test-access-modal-section-title">
            Доступ для классов
          </h3>
          <div className="test-access-classes-list">
            {classes.map((classItem) => {
              const access = classAccesses[classItem.id] || {
                isOpen: false,
                date: "",
              };
              return (
                <div
                  key={classItem.id}
                  className={`test-access-class-item ${access.isOpen ? "open" : ""}`}
                >
                  <div
                    className="test-access-class-icon"
                    style={{
                      background: access.isOpen ? "#10b981" : "#e5e7eb",
                      color: access.isOpen ? "#ffffff" : "#6b7280",
                    }}
                  >
                    {classItem.name}
                  </div>
                  <div className="test-access-class-info">
                    <div className="test-access-class-name">
                      {classItem.name}
                    </div>
                    <div className="test-access-class-subject">
                      {classItem.subject}
                    </div>
                    {access.date && (
                      <div className="test-access-class-status">
                        {access.isOpen ? "Открыт" : "Закрыт"}: {access.date}
                      </div>
                    )}
                  </div>
                  <button
                    className={`btn-toggle-access ${access.isOpen ? "close" : "open"}`}
                    onClick={() => handleToggle(classItem.id)}
                    type="button"
                  >
                    {access.isOpen ? (
                      <>
                        <ClosedLockIcon />
                        Закрыть
                      </>
                    ) : (
                      <>
                        <OpenLockIcon />
                        Открыть
                      </>
                    )}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

export default TestAccessModal;
