import { useState } from "react";
import "./ClassManagment.css";
import { HomeIcon } from "../../svgs/TeacherSvg.jsx";
import {
  UserPlusIcon,
  ExclamationIcon,
  CheckIcon,
  XIcon,
  GraduationCapIcon,
  BuildingIcon,
  UsersIcon,
  EmailIcon,
  PhoneIcon,
  CalendarIcon,
  ChevronDownIcon,
  ChevronUpIcon,
} from "../../svgs/ControlStudyClassSvg.jsx";
import { TrashIcon } from "../../svgs/ActivitySvg.jsx";

function ClassManagement({ onBackToMain }) {
  const [activeTab, setActiveTab] = useState("applications"); // "applications" or "classes"
  const [expandedClasses, setExpandedClasses] = useState({});

  const [applications] = useState([
    {
      id: 1,
      name: "Александров Артем",
      initials: "АА",
      email: "alexandrov@school.ru",
      phone: "+7 999 111 22 33",
      date: "04.02.2024",
      class: "9А",
    },
    {
      id: 2,
      name: "Белова Виктория",
      initials: "БВ",
      email: "belova@school.ru",
      phone: "+7 999 222 33 44",
      date: "03.02.2024",
      class: "9А",
    },
    {
      id: 3,
      name: "Григорьев Игорь",
      initials: "ГИ",
      email: "grigoriev.i@school.ru",
      phone: "+7 999 333 44 55",
      date: "03.02.2024",
      class: "10А",
    },
    {
      id: 4,
      name: "Денисова Кристина",
      initials: "ДК",
      email: "denisova@school.ru",
      phone: "+7 999 444 55 66",
      date: "02.02.2024",
      class: "9Б",
    },
    {
      id: 5,
      name: "Егоров Максим",
      initials: "ЕМ",
      email: "egorov@school.ru",
      phone: "+7 999 555 66 77",
      date: "01.02.2024",
      class: "10А",
    },
  ]);

  const [classes] = useState([
    {
      id: "9A",
      name: "9А",
      studentsCount: 3,
      course: "Математика",
      students: [
        {
          id: 1,
          name: "Петров Иван",
          initials: "ПИ",
          email: "petrov@school.ru",
          phone: "+7 999 123 45 67",
        },
        {
          id: 2,
          name: "Козлов Дмитрий",
          initials: "КД",
          email: "kozlov@school.ru",
          phone: "+7 999 345 67 89",
        },
        {
          id: 3,
          name: "Смирнова Анна",
          initials: "СА",
          email: "smirnova@school.ru",
          phone: "+7 999 234 56 78",
        },
      ],
    },
    {
      id: "9Б",
      name: "9Б",
      studentsCount: 2,
      course: "Математика",
      students: [
        {
          id: 4,
          name: "Новикова Елена",
          initials: "НЕ",
          email: "novikova@school.ru",
          phone: "+7 999 456 78 90",
        },
        {
          id: 5,
          name: "Морозов Алексей",
          initials: "МА",
          email: "morozov@school.ru",
          phone: "+7 999 567 89 01",
        },
      ],
    },
    {
      id: "10A",
      name: "10А",
      studentsCount: 3,
      course: "Алгебра",
      students: [
        {
          id: 6,
          name: "Волков Сергей",
          initials: "ВС",
          email: "volkov@school.ru",
          phone: "+7 999 678 90 12",
        },
        {
          id: 7,
          name: "Лебедева Мария",
          initials: "ЛМ",
          email: "lebedeva@school.ru",
          phone: "+7 999 789 01 23",
        },
        {
          id: 8,
          name: "Соколов Павел",
          initials: "СП",
          email: "sokolov@school.ru",
          phone: "+7 999 890 12 34",
        },
      ],
    },
  ]);

  const stats = {
    pendingApplications: applications.length,
    totalClasses: classes.length,
    totalStudents: classes.reduce(
      (sum, classItem) => sum + classItem.studentsCount,
      0,
    ),
  };

  const handleAcceptApplication = (applicationId) => {
    console.log("Accept application:", applicationId);
    // Здесь можно добавить логику принятия заявки
  };

  const handleDeclineApplication = (applicationId) => {
    console.log("Decline application:", applicationId);
    // Здесь можно добавить логику отклонения заявки
  };

  const handleDeleteStudent = (classId, studentId) => {
    console.log("Delete student:", classId, studentId);
    // Здесь можно добавить логику удаления ученика
  };

  const toggleClass = (classId) => {
    setExpandedClasses((prev) => ({
      ...prev,
      [classId]: !prev[classId],
    }));
  };

  return (
    <div className="class-management">
      <div className="class-management-container">
        <header className="class-management-header">
          <div className="class-management-header-left">
            <div className="class-management-header-icon">
              <UserPlusIcon />
            </div>
            <div>
              <h1 className="class-management-title">Управление учениками</h1>
              <p className="class-management-subtitle">
                Заявки на вступление и список учеников классов
              </p>
            </div>
          </div>
          <div className="class-management-header-actions">
            <button className="btn-home" onClick={onBackToMain} type="button">
              <HomeIcon />
              На главную
            </button>
          </div>
        </header>

        <div className="class-management-tabs">
          <button
            className={`tab-button ${activeTab === "applications" ? "active" : ""}`}
            onClick={() => setActiveTab("applications")}
            type="button"
          >
            <ExclamationIcon />
            Заявки на вступление
          </button>
          <button
            className={`tab-button ${activeTab === "classes" ? "active" : ""}`}
            onClick={() => setActiveTab("classes")}
            type="button"
          >
            <GraduationCapIcon />
            Классы и ученики
          </button>
        </div>

        <div className="class-management-stats">
          <div className="stat-card stat-orange">
            <div className="stat-icon">
              <ExclamationIcon />
            </div>
            <div className="stat-content">
              <div className="stat-label">Ожидающие заявки</div>
              <div className="stat-value">{stats.pendingApplications}</div>
              <div className="stat-subtitle">Требуют рассмотрения</div>
            </div>
          </div>
          <div className="stat-card stat-blue">
            <div className="stat-icon">
              <BuildingIcon />
            </div>
            <div className="stat-content">
              <div className="stat-label">Классов</div>
              <div className="stat-value">{stats.totalClasses}</div>
              <div className="stat-subtitle">Всего классов</div>
            </div>
          </div>
          <div className="stat-card stat-green">
            <div className="stat-icon">
              <UsersIcon />
            </div>
            <div className="stat-content">
              <div className="stat-label">Учеников</div>
              <div className="stat-value">{stats.totalStudents}</div>
              <div className="stat-subtitle">Всего учеников</div>
            </div>
          </div>
        </div>

        {activeTab === "applications" && (
          <section className="applications-section">
            <div className="applications-header">
              <div className="applications-header-icon">
                <ExclamationIcon />
              </div>
              <div>
                <h2 className="applications-title">Заявки на вступление</h2>
                <p className="applications-subtitle">
                  {stats.pendingApplications} заявок ожидают рассмотрения
                </p>
              </div>
            </div>
            <div className="applications-list">
              {applications.map((application) => (
                <div key={application.id} className="application-card">
                  <div
                    className="application-avatar"
                    style={{ background: "#f97316" }}
                  >
                    {application.initials}
                  </div>
                  <div className="application-info">
                    <div className="application-name">{application.name}</div>
                    <div className="application-details">
                      Заявка в класс {application.class}
                    </div>
                    <div className="application-contact">
                      <div className="contact-item">
                        <EmailIcon />
                        <span>{application.email}</span>
                      </div>
                      <div className="contact-item">
                        <PhoneIcon />
                        <span>{application.phone}</span>
                      </div>
                      <div className="contact-item">
                        <CalendarIcon />
                        <span>{application.date}</span>
                      </div>
                    </div>
                  </div>
                  <div className="application-actions">
                    <button
                      className="btn-accept"
                      onClick={() => handleAcceptApplication(application.id)}
                      type="button"
                    >
                      <CheckIcon />
                      Принять
                    </button>
                    <button
                      className="btn-decline"
                      onClick={() => handleDeclineApplication(application.id)}
                      type="button"
                    >
                      <XIcon />
                      Отклонить
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {activeTab === "classes" && (
          <section className="classes-section">
            <div className="classes-header">
              <div className="classes-header-icon">
                <GraduationCapIcon />
              </div>
              <div>
                <h2 className="classes-title">Классы и ученики</h2>
                <p className="classes-subtitle">
                  Управление учениками в классах
                </p>
              </div>
            </div>
            <div className="classes-list">
              {classes.map((classItem) => {
                const isExpanded = expandedClasses[classItem.id];
                return (
                  <div key={classItem.id} className="class-card-expandable">
                    <div
                      className="class-card-header-expandable"
                      onClick={() => toggleClass(classItem.id)}
                    >
                      <div className="class-header-left">
                        <div className="class-badge">{classItem.name}</div>
                        <div className="class-title">
                          Класс {classItem.name}
                        </div>
                      </div>
                      <div className="class-header-right">
                        <div className="class-info">
                          <div className="class-info-item">
                            <UsersIcon />
                            <span>{classItem.studentsCount} учеников</span>
                          </div>
                          <span className="class-info-separator">•</span>
                          <div className="class-info-item">
                            <span>Курс: {classItem.course}</span>
                          </div>
                        </div>
                        <button
                          className="btn-expand-class"
                          type="button"
                          aria-label={isExpanded ? "Свернуть" : "Развернуть"}
                        >
                          {isExpanded ? <ChevronUpIcon /> : <ChevronDownIcon />}
                        </button>
                      </div>
                    </div>
                    {isExpanded && (
                      <div className="class-students-content">
                        <div className="class-students-grid">
                          {classItem.students.map((student) => (
                            <div key={student.id} className="student-card">
                              <div
                                className="student-avatar"
                                style={{ background: "#3b82f6" }}
                              >
                                {student.initials}
                              </div>
                              <div className="student-info">
                                <div className="student-name">
                                  {student.name}
                                </div>
                                <div className="student-contact">
                                  <div className="contact-item">
                                    <EmailIcon />
                                    <span>{student.email}</span>
                                  </div>
                                  <div className="contact-item">
                                    <PhoneIcon />
                                    <span>{student.phone}</span>
                                  </div>
                                </div>
                              </div>
                              <button
                                className="btn-delete-student"
                                onClick={() =>
                                  handleDeleteStudent(classItem.id, student.id)
                                }
                                type="button"
                                aria-label="Delete"
                              >
                                <TrashIcon />
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

export default ClassManagement;
