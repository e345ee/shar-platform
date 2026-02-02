import { useState } from "react";
import "./Metodist.css";
import "./Profile/Profile"
import Teachers from "./Teacher/Teacher";
import StudyMaterial from "./StudyMaterial/StudyMaterial";

import {
  ProfileIcon,
  SettingsIcon,
  ActivitiesIcon,
  MaterialsIcon,
  TeachersIcon,
  AchievementsIcon,
  ClassesIcon,
} from "../../svgs/MethodistSvg"

import Profile from "./Profile/Profile";
import Class from "./Class/Class";
import Achievements from "./Achivment/Achivments";
import StudyActivity from "./StudyActivity/StudyActivity"

const menuCards = [
  {
    id: "activities",
    title: "Учебные активности",
    description:
        "Создавайте тесты с вопросами и правильными ответами, или текстовые задания для студентов",
    icon: ActivitiesIcon,
    tone: "blue",
  },
  {
    id: "materials",
    title: "Учебные материалы",
    description:
        "Управление учебными материалами: добавление, редактирование и удаление материалов к урокам",
    icon: MaterialsIcon,
    tone: "cyan",
  },
  {
    id: "teachers",
    title: "Преподаватели",
    description:
        "Добавление и управление преподавателями: имя, email, предмет преподавания",
    icon: TeachersIcon,
    tone: "green",
  },
  {
    id: "achievements",
    title: "Достижения",
    description:
        "Создание и настройка достижений для студентов с иконками и описаниями",
    icon: AchievementsIcon,
    tone: "yellow",
  },
  {
    id: "classes",
    title: "Учебные классы",
    description:
        "Управление классами: назначение преподавателя, расписание занятий, количество студентов",
    icon: ClassesIcon,
    tone: "purple",
  },
];

const stats = [
  {
    id: "activities",
    label: "Активностей",
    value: 1,
    tone: "blue",
  },
  {
    id: "materials",
    label: "Материалов",
    value: 0,
    tone: "blue",
  },
  {
    id: "teachers",
    label: "Преподавателей",
    value: 0,
    tone: "green",
  },
  {
    id: "classes",
    label: "Классов",
    value: 0,
    tone: "purple",
  },
];

function Methodist() {
  const [activeSection, setActiveSection] = useState(null);
  const [showProfile, setShowProfile] = useState(false);

  const handleLogout = () => {
    // Здесь можно добавить логику выхода
    console.log("Выход из системы");
  };

  if (showProfile) {
    return (
        <Profile
            onBackToMain={() => setShowProfile(false)}
            onLogout={handleLogout}
        />
    );
  }

  if (activeSection === "classes") {
    return (
        <Class
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "achievements") {
    return (
        <Achievements
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "teachers") {
    return (
        <Teachers
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "materials") {
    return (
        <StudyMaterial
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "activities") {
    return (
        <StudyActivity
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }


  return (
      <div className="dashboard">
        <div className="dashboard-shell">
          <header className="topbar">
            <div className="brand">
              <div className="brand-title">Панель методиста</div>
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

export default Methodist;
