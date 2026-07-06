import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import "./Teacher.css";
import { ProfileIcon, LogoutIcon, AchievementsIcon } from "../../svgs/MethodistSvg.jsx";
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
import AchievementAssignment from "./AchievementAssignment/AchievementAssignment";
import {
  listMyClasses,
  listPendingAttempts,
} from "../api/teacherApi";

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
  {
    id: "achievement-assignment",
    title: "Назначение достижений",
    description: "Выдача и снятие достижений ученикам в ваших классах",
    icon: AchievementsIcon,
    tone: "cyan",
  },
];

function Teacher({ onLogout }) {
  const containerVariants = {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: { staggerChildren: 0.08 } },
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 14 },
    show: { opacity: 1, y: 0 },
  };

  const gridVariants = {
    hidden: {},
    show: { transition: { staggerChildren: 0.06 } },
  };
  const [activeSection, setActiveSection] = useState(null);
  const [showProfile, setShowProfile] = useState(false);
  const [stats, setStats] = useState({
    classes: 0,
    students: 0,
    homework: 0,
    tests: 0,
  });
  const [isLoading, setIsLoading] = useState(true);

  const handleTopbarLogout = () => {
    if (typeof onLogout === "function") {
      onLogout();
      return;
    }
    localStorage.removeItem("auth_access_token");
    localStorage.removeItem("auth_role_name");
    window.location.href = "/login";
  };

  useEffect(() => {
    const loadStats = async () => {
      setIsLoading(true);
      try {
        const [classes, pendingAttempts] = await Promise.all([
          listMyClasses(),
          listPendingAttempts({ page: 0, size: 100 }),
        ]);


        let totalStudents = 0;
        const { listClassStudents } = await import("../api/teacherApi");
        for (const classItem of classes) {
          try {
            const studentsData = await listClassStudents(classItem.id, 0, 100);
            totalStudents += studentsData.totalElements || 0;
          } catch (e) {

          }
        }

        setStats({
          classes: classes.length,
          students: totalStudents,
          homework: pendingAttempts.totalElements || 0,
          tests: 0,
        });
      } catch (error) {
        console.error("Ошибка загрузки статистики:", error);
        setStats({
          classes: 0,
          students: 0,
          homework: 0,
          tests: 0,
        });
      } finally {
        setIsLoading(false);
      }
    };

    if (!activeSection && !showProfile) {
      loadStats();
    }
  }, [activeSection, showProfile]);

  if (showProfile) {
    return (
        <TeacherProfile
            onBackToMain={() => setShowProfile(false)}
            onLogout={onLogout}
        />
    );
  }

  if (activeSection === "edit-profile") {
    return (
        <TeacherProfile
            onBackToMain={() => setActiveSection(null)}
            onLogout={onLogout}
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

  if (activeSection === "achievement-assignment") {
    return <AchievementAssignment onBackToMain={() => setActiveSection(null)} />;
  }

  return (
      <div className="dashboard role-root role-teacher grain-overlay">
        <div className="dashboard-shell">
          <motion.header
              className="topbar"
              initial={{ opacity: 0, y: -12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
          >
            <div className="brand">
              <div className="brand-title">Панель преподавателя</div>
            </div>
            <div className="topbar-actions">
              <div className="topbar-action-group">
                <button
                    className="topbar-icon-btn"
                    type="button"
                    aria-label="Profile"
                    onClick={() => setShowProfile(true)}
                >
                  <ProfileIcon />
                </button>
              </div>
              <button
                  className="topbar-btn"
                  type="button"
                  onClick={handleTopbarLogout}
              >
                <LogoutIcon />
                Выйти
              </button>
            </div>
          </motion.header>

          <motion.main
              className="dashboard-content"
              variants={containerVariants}
              initial="hidden"
              animate="show"
          >
            <motion.section className="welcome-section" variants={itemVariants}>
              <h1 className="welcome-title">Добро пожаловать!</h1>
              <p className="welcome-subtitle">Выберите раздел для работы</p>
            </motion.section>

            <motion.section className="menu-grid" variants={gridVariants}>
              {menuCards.map((card) => {
                const Icon = card.icon;
                return (
                    <motion.button
                        key={card.id}
                        className={`menu-card tone-${card.tone}`}
                        onClick={() => setActiveSection(card.id)}
                        type="button"
                        variants={itemVariants}
                        whileHover={{ y: -3 }}
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
                    </motion.button>
                );
              })}
            </motion.section>

            <motion.section className="stats-section" variants={itemVariants}>
              <h2 className="stats-title">Статистика</h2>
              <div className="stats-grid">
                <div className="stat-item stat-item-common">
                  <div className="stat-value">{isLoading ? "..." : stats.classes}</div>
                  <div className="stat-label">Классов</div>
                </div>
                <div className="stat-item stat-item-common">
                  <div className="stat-value">{isLoading ? "..." : stats.students}</div>
                  <div className="stat-label">Студентов</div>
                </div>
                <div className="stat-item stat-item-common">
                  <div className="stat-value">{isLoading ? "..." : stats.homework}</div>
                  <div className="stat-label">Заданий на проверку</div>
                </div>
                <div className="stat-item stat-item-common">
                  <div className="stat-value">{isLoading ? "..." : stats.tests}</div>
                  <div className="stat-label">Активных тестов</div>
                </div>
              </div>
            </motion.section>
          </motion.main>
        </div>
      </div>
  );
}

export default Teacher;
