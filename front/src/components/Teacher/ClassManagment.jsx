import { useState, useEffect } from "react";
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
import { PlusIcon } from "../../svgs/MethodistSvg.jsx";
import {
  listMyClasses,
  listJoinRequests,
  approveJoinRequest,
  rejectJoinRequest,
  removeStudentFromClass,
  listClassStudents,
} from "../api/teacherApi";

function ClassManagement({ onBackToMain }) {
  const [activeTab, setActiveTab] = useState("applications"); // "applications" or "classes"
  const [expandedClasses, setExpandedClasses] = useState({});
  const [classes, setClasses] = useState([]);
  const [applications, setApplications] = useState([]);
  const [studentsByClass, setStudentsByClass] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [showAddStudentModal, setShowAddStudentModal] = useState(false);
  const [copiedCode, setCopiedCode] = useState(null);

  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const classesData = await listMyClasses();
        setClasses(classesData);

        // Загружаем заявки для всех классов
        const allApplications = [];
        for (const classItem of classesData) {
          try {
            const requests = await listJoinRequests(classItem.id);
            allApplications.push(...requests);
          } catch (e) {
            console.error(`Ошибка загрузки заявок для класса ${classItem.id}:`, e);
          }
        }
        setApplications(allApplications);

        // Загружаем студентов для всех классов
        const studentsMap = {};
        for (const classItem of classesData) {
          try {
            const studentsData = await listClassStudents(classItem.id, 0, 100);
            studentsMap[classItem.id] = studentsData.content || [];
          } catch (e) {
            console.error(`Ошибка загрузки студентов для класса ${classItem.id}:`, e);
            studentsMap[classItem.id] = [];
          }
        }
        setStudentsByClass(studentsMap);
      } catch (error) {
        setErrorMessage(error?.message || "Не удалось загрузить данные");
        console.error("Ошибка загрузки данных:", error);
      } finally {
        setIsLoading(false);
      }
    };

    loadData();
  }, []);

  const stats = {
    pendingApplications: applications.length,
    totalClasses: classes.length,
    totalStudents: Object.values(studentsByClass).reduce(
        (sum, students) => sum + students.length,
        0,
    ),
  };

  const getInitials = (name) => {
    if (!name) return "??";
    const parts = name.trim().split(" ");
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("ru-RU", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch (e) {
      return dateString;
    }
  };

  const handleAcceptApplication = async (requestId, classId) => {
    setErrorMessage("");
    try {
      await approveJoinRequest(requestId, classId);
      // Обновляем заявки
      const updatedApplications = [];
      for (const classItem of classes) {
        try {
          const requests = await listJoinRequests(classItem.id);
          updatedApplications.push(...requests);
        } catch (e) {
          console.error(`Ошибка загрузки заявок для класса ${classItem.id}:`, e);
        }
      }
      setApplications(updatedApplications);
      // Обновляем студентов класса
      const studentsData = await listClassStudents(classId, 0, 100);
      setStudentsByClass((prev) => ({
        ...prev,
        [classId]: studentsData.content || [],
      }));
      // Обновляем статистику
      const newStats = {
        pendingApplications: updatedApplications.length,
        totalClasses: classes.length,
        totalStudents: Object.values({ ...studentsByClass, [classId]: studentsData.content || [] }).reduce(
            (sum, students) => sum + students.length,
            0,
        ),
      };
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось принять заявку");
    }
  };

  const handleDeclineApplication = async (requestId, classId) => {
    setErrorMessage("");
    try {
      await rejectJoinRequest(requestId, classId);
      setApplications((prev) => prev.filter((req) => req.id !== requestId));
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось отклонить заявку");
    }
  };

  const handleDeleteStudent = async (classId, studentId) => {
    if (!window.confirm("Вы уверены, что хотите удалить этого ученика из класса?")) {
      return;
    }
    setErrorMessage("");
    try {
      await removeStudentFromClass(classId, studentId);
      // Обновляем список студентов
      const studentsData = await listClassStudents(classId, 0, 100);
      setStudentsByClass((prev) => ({
        ...prev,
        [classId]: studentsData.content || [],
      }));
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось удалить студента");
    }
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
              <button
                  className="btn-add-student"
                  onClick={() => setShowAddStudentModal(true)}
                  type="button"
              >
                <PlusIcon />
                Добавить ученика
              </button>
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

          {errorMessage && (
              <div className="class-management-error">{errorMessage}</div>
          )}

          {isLoading ? (
              <div className="class-management-loading">Загрузка...</div>
          ) : (
              <>
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
              </>
          )}

          {activeTab === "applications" && !isLoading && (
              <section className="applications-section">
                <div className="applications-header">
                  <div className="applications-header-icon">
                    <ExclamationIcon />
                  </div>
                  <div>
                    <h2 className="applications-title">Заявки на вступление</h2>
                    <p className="applications-subtitle">
                      {applications.length === 0
                          ? "Нет заявок на рассмотрение"
                          : `${applications.length} ${applications.length === 1 ? 'заявка' : applications.length < 5 ? 'заявки' : 'заявок'} ожидают рассмотрения`}
                    </p>
                  </div>
                </div>
                <div className="applications-list">
                  {applications.length === 0 ? (
                      <div className="applications-empty">
                        <ExclamationIcon />
                        <p>Заявок пока нет</p>
                        <span>Все заявки на вступление в классы будут отображаться здесь</span>
                      </div>
                  ) : (
                      applications.map((application) => (
                          <div key={application.id} className="application-card">
                            <div
                                className="application-avatar"
                                style={{ background: "#f97316" }}
                            >
                              {getInitials(application.studentName)}
                            </div>
                            <div className="application-info">
                              <div className="application-name">
                                {application.studentName || "Не указано"}
                              </div>
                              <div className="application-details">
                                Заявка в класс <strong>{application.className || "Не указан"}</strong>
                              </div>
                              <div className="application-contact">
                                <div className="contact-item">
                                  <EmailIcon />
                                  <span>{application.studentEmail || "Не указан"}</span>
                                </div>
                                <div className="contact-item">
                                  <CalendarIcon />
                                  <span>{formatDate(application.createdAt) || "Не указана"}</span>
                                </div>
                              </div>
                            </div>
                            <div className="application-actions">
                              <button
                                  className="btn-accept"
                                  onClick={() =>
                                      handleAcceptApplication(application.id, application.classId)
                                  }
                                  type="button"
                              >
                                <CheckIcon />
                                Принять
                              </button>
                              <button
                                  className="btn-decline"
                                  onClick={() =>
                                      handleDeclineApplication(application.id, application.classId)
                                  }
                                  type="button"
                              >
                                <XIcon />
                                Отклонить
                              </button>
                            </div>
                          </div>
                      ))
                  )}
                </div>
              </section>
          )}

          {activeTab === "classes" && !isLoading && (
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
                  {classes.length === 0 ? (
                      <div className="classes-empty">
                        <GraduationCapIcon />
                        <p>Классов пока нет</p>
                        <span>Ваши классы будут отображаться здесь</span>
                      </div>
                  ) : (
                      classes.map((classItem) => {
                        const isExpanded = expandedClasses[classItem.id];
                        const students = studentsByClass[classItem.id] || [];
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
                                      <span>{students.length} учеников</span>
                                    </div>
                                    <span className="class-info-separator">•</span>
                                    <div className="class-info-item">
                                      <span>Курс: {classItem.courseName || "Не указан"}</span>
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
                                    {students.length === 0 ? (
                                        <div className="students-empty">
                                          <UsersIcon />
                                          <p>В классе пока нет учеников</p>
                                          <span>Ученики, добавленные в этот класс, будут отображаться здесь</span>
                                        </div>
                                    ) : (
                                        <div className="class-students-grid">
                                          {students.map((student) => (
                                              <div key={student.id} className="student-card">
                                                <div
                                                    className="student-avatar"
                                                    style={{ background: "#3b82f6" }}
                                                >
                                                  {getInitials(student.name)}
                                                </div>
                                                <div className="student-info">
                                                  <div className="student-name">
                                                    {student.name || "Не указано"}
                                                  </div>
                                                  <div className="student-contact">
                                                    <div className="contact-item">
                                                      <EmailIcon />
                                                      <span>{student.email || "Не указан"}</span>
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
                                    )}
                                  </div>
                              )}
                            </div>
                        );
                      })
                  )}
                </div>
              </section>
          )}

          {showAddStudentModal && (
              <div className="modal-overlay" onClick={() => setShowAddStudentModal(false)}>
                <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                  <button
                      className="modal-close"
                      onClick={() => setShowAddStudentModal(false)}
                      type="button"
                      aria-label="Закрыть"
                  >
                    <XIcon />
                  </button>
                  <h2 className="modal-title">Добавление ученика в класс</h2>
                  <p className="modal-subtitle">
                    Ученики могут присоединиться к классу, используя код класса.
                    Отправьте код класса ученику, чтобы он мог подать заявку на вступление.
                  </p>

                  <div className="modal-class-codes">
                    <h3>Коды классов:</h3>
                    {classes.length === 0 ? (
                        <p className="modal-empty">У вас пока нет классов</p>
                    ) : (
                        <div className="class-codes-list">
                          {classes.map((classItem) => (
                              <div key={classItem.id} className="class-code-item">
                                <div className="class-code-info">
                                  <strong>{classItem.name}</strong>
                                  {classItem.courseName && (
                                      <span className="class-code-course">{classItem.courseName}</span>
                                  )}
                                </div>
                                <div className="class-code-wrapper">
                                  <code className="class-code">{classItem.joinCode || "Не указан"}</code>
                                  <button
                                      className="btn-copy-code"
                                      onClick={() => {
                                        if (classItem.joinCode) {
                                          navigator.clipboard.writeText(classItem.joinCode);
                                          setCopiedCode(classItem.id);
                                          setTimeout(() => setCopiedCode(null), 2000);
                                        }
                                      }}
                                      type="button"
                                      disabled={!classItem.joinCode}
                                  >
                                    {copiedCode === classItem.id ? "Скопировано!" : "Копировать"}
                                  </button>
                                </div>
                              </div>
                          ))}
                        </div>
                    )}
                  </div>

                  <div className="modal-actions">
                    <button
                        className="btn-secondary"
                        onClick={() => setShowAddStudentModal(false)}
                        type="button"
                    >
                      Закрыть
                    </button>
                  </div>
                </div>
              </div>
          )}
        </div>
      </div>
  );
}

export default ClassManagement;
