import { useEffect, useMemo, useState } from "react";
import "./Metodist.css";
import "./Profile/Profile"
import Teachers from "./Teacher/Teacher";
import StudyMaterial from "./StudyMaterial/StudyMaterial";

import {
  LogoutIcon,
  ProfileIcon,
  SettingsIcon,
  ActivitiesIcon,
  MaterialsIcon,
  TeachersIcon,
  AchievementsIcon,
  ClassesIcon,
} from "../../svgs/MethodistSvg"

import Profile from "./Profile/Profile";
import Class from "./ClassAndCourses/Class";
import Achievements from "./Achivment/Achivments";
import StudyActivity from "./StudyActivity/StudyActivity"
import {
  listActivitiesByLesson,
  listLessonsByCourse,
  listMyClasses,
  listMyCourses,
  listTeachers,
  listWeeklyActivitiesByCourse,
} from "../api/methodistApi";

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
        "Добавление и управление преподавателями: имя, email, пароль, telegram id",
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
    title: "Курсы и классы",
    description:
        "Управление курсами и классами: создание курсов, классов и назначение преподавателей",
    icon: ClassesIcon,
    tone: "purple",
  },
];

function Methodist({ onLogout }) {
  const [activeSection, setActiveSection] = useState(null);
  const [showProfile, setShowProfile] = useState(false);
  const [statsData, setStatsData] = useState({
    activities: 0,
    materials: 0,
    teachers: 0,
    classes: 0,
  });

  useEffect(() => {
    if (activeSection || showProfile) {
      return undefined;
    }
    let isCancelled = false;

    const loadStats = async () => {
      try {
        const [teachers, classes, courses] = await Promise.all([
          listTeachers(0, 500),
          listMyClasses(),
          listMyCourses(),
        ]);

        let lessonsCount = 0;
        const activityIds = new Set();

        for (const course of courses) {
          const lessons = await listLessonsByCourse(course.id);
          lessonsCount += lessons.length;

          const perLesson = await Promise.all(
              lessons.map((lesson) => listActivitiesByLesson(lesson.id))
          );
          perLesson.flat().forEach((activity) => activityIds.add(activity.id));

          const weekly = await listWeeklyActivitiesByCourse(course.id);
          weekly.forEach((activity) => activityIds.add(activity.id));
        }

        if (!isCancelled) {
          setStatsData({
            activities: activityIds.size,
            materials: lessonsCount,
            teachers: teachers.length,
            classes: classes.length,
          });
        }
      } catch (e) {
        if (!isCancelled) {
          setStatsData({
            activities: 0,
            materials: 0,
            teachers: 0,
            classes: 0,
          });
        }
      }
    };

    loadStats();
    return () => {
      isCancelled = true;
    };
  }, [activeSection, showProfile]);

  const stats = useMemo(
      () => [
        {
          id: "activities",
          label: "Активностей",
          value: statsData.activities,
          tone: "blue",
        },
        {
          id: "materials",
          label: "Материалов",
          value: statsData.materials,
          tone: "blue",
        },
        {
          id: "teachers",
          label: "Преподавателей",
          value: statsData.teachers,
          tone: "green",
        },
        {
          id: "classes",
          label: "Классов",
          value: statsData.classes,
          tone: "purple",
        },
      ],
      [statsData]
  );

  if (showProfile) {
    return (
        <Profile
            onBackToMain={() => setShowProfile(false)}
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
                  aria-label="Logout"
                  onClick={onLogout}
              >
                <LogoutIcon />
              </button>
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
