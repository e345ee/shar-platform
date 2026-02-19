import { useEffect, useMemo, useState } from "react";
import "./Student.css";
import {
  LogoutIcon,
  ProfileIcon,
  SettingsIcon,
  ActivitiesIcon,
  MaterialsIcon,
  AchievementsIcon,
  ClassesIcon,
} from "../../svgs/MethodistSvg";
import Profile from "./Profile/Profile";
import Statistics from "./Statistics/Statistics";
import ClassFeed from "./ClassFeed/ClassFeed";
import StudentAchievements from "./StudentAchievements/StudentAchievements";
import StudyMaterials from "./StudyMaterials/StudyMaterials";
import StudyActivities from "./StudyActivities/StudyActivities";
import ActivityResults from "./ActivityResults/ActivityResults";
import Certificate from "./Certificate/Certificate";
import MyClasses from "./MyClasses/MyClasses";
import {
  getMyStatisticsOverview,
  listMyCourses,
} from "../api/studentApi";

const menuCards = [
  {
    id: "statistics",
    title: "Статистика",
    description:
        "Просмотр вашей статистики: результаты учебной активности, количество выполненных заданий, достижения и прогресс в обучении",
    icon: ActivitiesIcon,
    tone: "blue",
  },
  {
    id: "classFeed",
    title: "Лента достижений класса",
    description:
        "Просмотр ленты достижений класса, где отображаются все достижения, полученные в процессе обучения каждого ученика в классе",
    icon: AchievementsIcon,
    tone: "yellow",
  },
  {
    id: "personalAchievements",
    title: "Личные достижения",
    description:
        "Просмотр ваших личных достижений и рекомендаций по получению новых",
    icon: AchievementsIcon,
    tone: "yellow",
  },
  {
    id: "studyMaterials",
    title: "Учебные материалы",
    description:
        "Просмотр учебных материалов, опубликованных методистом",
    icon: MaterialsIcon,
    tone: "cyan",
  },
  {
    id: "studyActivities",
    title: "Выполнение активности",
    description:
        "Выполнение учебных активностей: тесты, домашние задания, еженедельные задания, задачи для отстающих учеников",
    icon: ActivitiesIcon,
    tone: "blue",
  },
  {
    id: "activityResults",
    title: "Результаты активности",
    description:
        "Автоматическое получение результатов на учебные активности с автоматической проверкой и мгновенной обратной связью",
    icon: ActivitiesIcon,
    tone: "green",
  },
  {
    id: "certificate",
    title: "Сертификат",
    description:
        "Получение сертификата после прохождения курса и отправка его на почту",
    icon: AchievementsIcon,
    tone: "purple",
  },
  {
    id: "myClasses",
    title: "Мои классы и заявки",
    description:
        "Подача заявки на вступление в класс и просмотр информации о ваших классах",
    icon: ClassesIcon,
    tone: "cyan",
  },
];

function Student({ onLogout }) {
  const [activeSection, setActiveSection] = useState(null);
  const [showProfile, setShowProfile] = useState(false);
  const [statsData, setStatsData] = useState({
    courses: 0,
    attempts: 0,
    achievements: 0,
    completed: 0,
  });

  useEffect(() => {
    if (activeSection || showProfile) {
      return undefined;
    }
    let isCancelled = false;

    const loadStats = async () => {
      try {
        const [courses, stats] = await Promise.all([
          listMyCourses(),
          getMyStatisticsOverview(),
        ]);

        if (!isCancelled) {
          setStatsData({
            courses: courses.length,
            attempts: stats?.attemptsTotal || 0,
            achievements: stats?.testsGraded || 0,
            completed: stats?.coursesCompleted || 0,
          });
        }
      } catch (e) {
        if (!isCancelled) {
          setStatsData({
            courses: 0,
            attempts: 0,
            achievements: 0,
            completed: 0,
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
          id: "courses",
          label: "Курсов",
          value: statsData.courses,
          tone: "blue",
        },
        {
          id: "attempts",
          label: "Попыток",
          value: statsData.attempts,
          tone: "blue",
        },
        {
          id: "achievements",
          label: "Достижений",
          value: statsData.achievements,
          tone: "yellow",
        },
        {
          id: "completed",
          label: "Завершено курсов",
          value: statsData.completed,
          tone: "green",
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

  if (activeSection === "statistics") {
    return (
        <Statistics
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "myClasses") {
    return (
        <MyClasses
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "classFeed") {
    return (
        <ClassFeed
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "personalAchievements") {
    return (
        <StudentAchievements
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "studyMaterials") {
    return (
        <StudyMaterials
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "studyActivities") {
    return (
        <StudyActivities
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "activityResults") {
    return (
        <ActivityResults
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "certificate") {
    return (
        <Certificate
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  if (activeSection === "myClasses") {
    return (
        <MyClasses
            onBackToMain={() => setActiveSection(null)}
        />
    );
  }

  return (
      <div className="dashboard">
        <div className="dashboard-shell">
          <header className="topbar">
            <div className="brand">
              <div className="brand-title">Панель студента</div>
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

export default Student;
