// import { useEffect, useState } from "react";
// import "../Metodist/Metodist.css";
// import "./Student.css";
// import {
//     ActivitiesIcon,
//     AchievementsIcon,
//     ClassesIcon,
//     LogoutIcon,
//     MaterialsIcon,
// } from "../../svgs/MethodistSvg";
// import { getMyAchievementsPage, getMyStatisticsOverview, listMyCourses } from "../api/studentApi";
// import StudyModule from "./modules/StudyModule";
// import StatisticsModule from "./modules/StatisticsModule";
// import ClassFeedModule from "./modules/ClassFeedModule";
// import PersonalAchievementsModule from "./modules/PersonalAchievementsModule";
// import JoinClassModule from "./modules/JoinClassModule";
//
// const SECTION_STUDY = "study";
// const SECTION_STATS = "stats";
// const SECTION_CLASS_FEED = "classFeed";
// const SECTION_PERSONAL_ACHIEVEMENTS = "personalAchievements";
// const SECTION_JOIN_CLASS = "joinClass";
//
// const MENU_CARDS = [
//     {
//         id: SECTION_STUDY,
//         title: "Учебные материалы и активности",
//         description:
//             "Просмотр материалов, выполнение тестов и заданий, мгновенная обратная связь и отправка сертификата на почту.",
//         icon: MaterialsIcon,
//         tone: "blue",
//     },
//     {
//         id: SECTION_STATS,
//         title: "Статистика ученика",
//         description: "Результаты активностей, прогресс по курсам и выполненные задания.",
//         icon: ClassesIcon,
//         tone: "purple",
//     },
//     {
//         id: SECTION_CLASS_FEED,
//         title: "Лента достижений класса",
//         description: "Просмотр достижений всех учеников вашего класса.",
//         icon: ActivitiesIcon,
//         tone: "cyan",
//     },
//     {
//         id: SECTION_PERSONAL_ACHIEVEMENTS,
//         title: "Личные достижения",
//         description: "Ваши достижения и рекомендации для получения новых наград.",
//         icon: AchievementsIcon,
//         tone: "yellow",
//     },
//     {
//         id: SECTION_JOIN_CLASS,
//         title: "Вступление в класс",
//         description: "Отправка заявки на вступление в класс по коду.",
//         icon: ActivitiesIcon,
//         tone: "green",
//     },
// ];
//
// function SectionShell({ title, onBack, onLogout, children }) {
//     return (
//         <div className="dashboard">
//             <div className="dashboard-shell">
//                 <header className="topbar">
//                     <div className="brand">
//                         <div className="brand-title">{title}</div>
//                     </div>
//                     <div className="topbar-actions">
//                         <button className="student-back-btn" type="button" onClick={onBack}>
//                             Назад
//                         </button>
//                         <button className="topbar-icon-btn" type="button" aria-label="Logout" onClick={onLogout}>
//                             <LogoutIcon />
//                         </button>
//                     </div>
//                 </header>
//                 <main className="dashboard-content student-section-content">{children}</main>
//             </div>
//         </div>
//     );
// }
//
// function Student({ onLogout }) {
//     const [activeSection, setActiveSection] = useState(null);
//     const [dashboardStats, setDashboardStats] = useState({
//         courses: 0,
//         achievements: 0,
//         attemptsFinished: 0,
//         testsGraded: 0,
//     });
//
//     useEffect(() => {
//         if (activeSection !== null) return;
//         let isCancelled = false;
//
//         const loadDashboard = async () => {
//             try {
//                 const [courses, achievements, overview] = await Promise.all([
//                     listMyCourses(),
//                     getMyAchievementsPage(),
//                     getMyStatisticsOverview(),
//                 ]);
//
//                 if (!isCancelled) {
//                     setDashboardStats({
//                         courses: Array.isArray(courses) ? courses.length : 0,
//                         achievements: achievements?.totalEarned ?? 0,
//                         attemptsFinished: overview?.attemptsFinished ?? 0,
//                         testsGraded: overview?.testsGraded ?? 0,
//                     });
//                 }
//             } catch (e) {
//                 if (!isCancelled) {
//                     setDashboardStats({
//                         courses: 0,
//                         achievements: 0,
//                         attemptsFinished: 0,
//                         testsGraded: 0,
//                     });
//                 }
//             }
//         };
//
//         loadDashboard();
//         return () => {
//             isCancelled = true;
//         };
//     }, [activeSection]);
//
//     if (activeSection === SECTION_STUDY) {
//         return (
//             <SectionShell
//                 title="Учебные материалы и активности"
//                 onBack={() => setActiveSection(null)}
//                 onLogout={onLogout}
//             >
//                 <StudyModule />
//             </SectionShell>
//         );
//     }
//
//     if (activeSection === SECTION_STATS) {
//         return (
//             <SectionShell title="Статистика ученика" onBack={() => setActiveSection(null)} onLogout={onLogout}>
//                 <StatisticsModule />
//             </SectionShell>
//         );
//     }
//
//     if (activeSection === SECTION_CLASS_FEED) {
//         return (
//             <SectionShell
//                 title="Лента достижений класса"
//                 onBack={() => setActiveSection(null)}
//                 onLogout={onLogout}
//             >
//                 <ClassFeedModule />
//             </SectionShell>
//         );
//     }
//
//     if (activeSection === SECTION_PERSONAL_ACHIEVEMENTS) {
//         return (
//             <SectionShell
//                 title="Личные достижения"
//                 onBack={() => setActiveSection(null)}
//                 onLogout={onLogout}
//             >
//                 <PersonalAchievementsModule />
//             </SectionShell>
//         );
//     }
//
//     if (activeSection === SECTION_JOIN_CLASS) {
//         return (
//             <SectionShell title="Вступление в класс" onBack={() => setActiveSection(null)} onLogout={onLogout}>
//                 <JoinClassModule />
//             </SectionShell>
//         );
//     }
//
//     return (
//         <div className="dashboard">
//             <div className="dashboard-shell">
//                 <header className="topbar">
//                     <div className="brand">
//                         <div className="brand-title">Панель ученика</div>
//                     </div>
//                     <div className="topbar-actions">
//                         <button className="topbar-icon-btn" type="button" aria-label="Logout" onClick={onLogout}>
//                             <LogoutIcon />
//                         </button>
//                     </div>
//                 </header>
//
//                 <main className="dashboard-content">
//                     <section className="welcome-section">
//                         <h1 className="welcome-title">Добро пожаловать!</h1>
//                         <p className="welcome-subtitle">Выберите модуль для работы</p>
//                     </section>
//
//                     <section className="menu-grid">
//                         {MENU_CARDS.map((card) => {
//                             const Icon = card.icon;
//                             return (
//                                 <button
//                                     key={card.id}
//                                     className={`menu-card tone-${card.tone}`}
//                                     onClick={() => setActiveSection(card.id)}
//                                     type="button"
//                                 >
//                                     <div className={`menu-icon tone-${card.tone}`}>
//                                         <Icon />
//                                     </div>
//                                     <div className="menu-content">
//                                         <h3 className="menu-title">{card.title}</h3>
//                                         <p className="menu-description">{card.description}</p>
//                                     </div>
//                                     <div className={`menu-link tone-${card.tone}`}>Перейти к работе →</div>
//                                 </button>
//                             );
//                         })}
//                     </section>
//
//                     <section className="stats-section">
//                         <h2 className="stats-title">Статистика</h2>
//                         <div className="stats-grid">
//                             <div className="stat-item stat-item-common">
//                                 <div className="stat-value">{dashboardStats.courses}</div>
//                                 <div className="stat-label">Моих курсов</div>
//                             </div>
//                             <div className="stat-item stat-item-common">
//                                 <div className="stat-value">{dashboardStats.achievements}</div>
//                                 <div className="stat-label">Личных достижений</div>
//                             </div>
//                             <div className="stat-item stat-item-common">
//                                 <div className="stat-value">{dashboardStats.attemptsFinished}</div>
//                                 <div className="stat-label">Завершенных попыток</div>
//                             </div>
//                             <div className="stat-item stat-item-common">
//                                 <div className="stat-value">{dashboardStats.testsGraded}</div>
//                                 <div className="stat-label">Проверенных тестов</div>
//                             </div>
//                         </div>
//                     </section>
//                 </main>
//             </div>
//         </div>
//     );
// }
//
// export default Student;
