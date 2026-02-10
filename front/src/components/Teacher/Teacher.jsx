import { useState } from "react";
import "./Teacher.css";
import { ProfileIcon, SettingsIcon } from "../../svgs/MethodistSvg.jsx";
import {
  ClassManagementIcon,
  HomeworkCheckIcon,
  TestAccessIcon,
  EditProfileIcon,
  NotificationsIcon,
} from "../../svgs/TeacherSvg.jsx";
import TeacherProfile from "./TeacherProfile/TeacherProfile";
import Notifications from "./Notifications/Notifications";
import HomeworkCheck from "./HomeworkCheck/HomeworkCheck";
import TestAccess from "./TestAcess/TestAcess";
import ClassManagement from "./ClassManagment";

const menuCards = [
  {
    id: "class-management",
    title: "Управление учебным классом",
    description:
      "Просмотр списка студентов, управление классом, назначение заданий",
    icon: ClassManagementIcon,
    tone: "blue",
  },
  {
    id: "homework-check",
    title: "Проверка домашних заданий",
    description: "Проверка и оценка выполненных домашних заданий студентов",
    icon: HomeworkCheckIcon,
    tone: "green",
  },
  {
    id: "test-access",
    title: "Открыть доступ к тесту",
    description: "Управление доступом студентов к тестам и контрольным работам",
    icon: TestAccessIcon,
    tone: "purple",
  },
  {
    id: "edit-profile",
    title: "Редактировать профиль",
    description: "Изменение личной информации, контактов и настроек профиля",
    icon: EditProfileIcon,
    tone: "yellow",
  },
  {
    id: "notifications",
    title: "Получение уведомлений",
    description:
      "Просмотр уведомлений о новых заданиях, ответах студентов и важных событиях",
    icon: NotificationsIcon,
    tone: "orange",
  },
];

const stats = [
  {
    id: "classes",
    label: "Классов",
    value: 2,
    tone: "blue",
  },
  {
    id: "students",
    label: "Студентов",
    value: 45,
    tone: "green",
  },
  {
    id: "homework",
    label: "Заданий на проверку",
    value: 12,
    tone: "orange",
  },
  {
    id: "tests",
    label: "Активных тестов",
    value: 3,
    tone: "purple",
  },
];

function Teacher() {
  const [activeSection, setActiveSection] = useState(null);
  const [showProfile, setShowProfile] = useState(false);

  const handleLogout = () => {
    console.log("Выход из системы");
  };

  if (showProfile) {
    return (
      <TeacherProfile
        onBackToMain={() => setShowProfile(false)}
        onLogout={handleLogout}
      />
    );
  }

  if (activeSection === "edit-profile") {
    return (
      <TeacherProfile
        onBackToMain={() => setActiveSection(null)}
        onLogout={handleLogout}
      />
    );
  }

  if (activeSection === "notifications") {
    return <Notifications onBackToMain={() => setActiveSection(null)} />;
  }

  if (activeSection === "homework-check") {
    return <HomeworkCheck onBackToMain={() => setActiveSection(null)} />;
  }

  if (activeSection === "test-access") {
    return <TestAccess onBackToMain={() => setActiveSection(null)} />;
  }

  if (activeSection === "class-management") {
    return <ClassManagement onBackToMain={() => setActiveSection(null)} />;
  }

  return (
    <div className="dashboard">
      <div className="dashboard-shell">
        <header className="topbar">
          <div className="brand">
            <div className="brand-title">Панель преподавателя</div>
          </div>
          <div className="topbar-actions">
            <button
              className="topbar-icon-btn"
              type="button"
              aria-label="Profile"
              onClick={() => setShowProfile(true)}
            >
              <ProfileIcon />
            </button>
            <button
              className="topbar-icon-btn"
              type="button"
              aria-label="Settings"
            >
              <SettingsIcon />
            </button>
          </div>
        </header>

        <main className="dashboard-content">
          <section className="welcome-section">
            <h1 className="welcome-title">Добро пожаловать!</h1>
            <p className="welcome-subtitle">Выберите раздел для работы</p>
          </section>

          <section className="menu-grid">
            {menuCards.map((card) => {
              const Icon = card.icon;
              return (
                <button
                  key={card.id}
                  className={`menu-card tone-${card.tone}`}
                  onClick={() => setActiveSection(card.id)}
                  type="button"
                >
                  <div className={`menu-icon tone-${card.tone}`}>
                    <Icon />
                  </div>
                  <div className="menu-content">
                    <h3 className="menu-title">{card.title}</h3>
                    <p className="menu-description">{card.description}</p>
                  </div>
                  <div className={`menu-link tone-${card.tone}`}>
                    Перейти к работе →
                  </div>
                </button>
              );
            })}
          </section>

          <section className="stats-section">
            <h2 className="stats-title">Статистика</h2>
            <div className="stats-grid">
              {stats.map((stat) => (
                <div key={stat.id} className="stat-item stat-item-common">
                  <div className="stat-value">{stat.value}</div>
                  <div className="stat-label">{stat.label}</div>
                </div>
              ))}
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}

export default Teacher;
