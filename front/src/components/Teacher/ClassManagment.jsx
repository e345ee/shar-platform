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
  closeCourseForStudent,
  listClassStudents,
  createStudent,
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
  const [showClassCodesModal, setShowClassCodesModal] = useState(false);
  const [isCreatingStudent, setIsCreatingStudent] = useState(false);
  const [createdStudentMessage, setCreatedStudentMessage] = useState("");
  const [copiedCode, setCopiedCode] = useState(null);
  const [closingCourseKey, setClosingCourseKey] = useState("");
  const [studentForm, setStudentForm] = useState({
    name: "",
    email: "",
    password: "",
    tgId: "",
  });

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

  const handleCloseCourseForStudent = async (classId, studentId, studentName) => {
    const confirmText = `Отметить курс как пройденный для ученика "${studentName || "Без имени"}"?`;
    if (!window.confirm(confirmText)) {
      return;
    }

    const actionKey = `${classId}:${studentId}`;
    setClosingCourseKey(actionKey);
    setErrorMessage("");

    try {
      await closeCourseForStudent(classId, studentId);
      const studentsData = await listClassStudents(classId, 0, 100);
      setStudentsByClass((prev) => ({
        ...prev,
        [classId]: studentsData.content || [],
      }));
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось отметить прохождение курса");
    } finally {
      setClosingCourseKey("");
    }
  };

  const toggleClass = (classId) => {
    setExpandedClasses((prev) => ({
      ...prev,
      [classId]: !prev[classId],
    }));
  };

  const handleStudentFormChange = (field, value) => {
    setStudentForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleCreateStudent = async (e) => {
    e.preventDefault();
    setErrorMessage("");
    setCreatedStudentMessage("");
    setIsCreatingStudent(true);
    try {
      const payload = {
        name: studentForm.name.trim(),
        email: studentForm.email.trim(),
        password: studentForm.password,
        tgId: studentForm.tgId.trim() || null,
      };
      const created = await createStudent(payload);
      setCreatedStudentMessage(`Ученик ${created?.name || payload.name} успешно создан`);
      setStudentForm({
        name: "",
        email: "",
        password: "",
        tgId: "",
      });
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось создать ученика");
    } finally {
      setIsCreatingStudent(false);
    }
  };

  const handleCopyCode = async (code) => {
    try {
      await navigator.clipboard.writeText(code);
      setCopiedCode(code);
      setTimeout(() => setCopiedCode(null), 2000);
    } catch (err) {
      // Fallback для старых браузеров
      const textArea = document.createElement("textarea");
      textArea.value = code;
      textArea.style.position = "fixed";
      textArea.style.opacity = "0";
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand("copy");
        setCopiedCode(code);
        setTimeout(() => setCopiedCode(null), 2000);
      } catch (e) {
        console.error("Не удалось скопировать код", e);
      }
      document.body.removeChild(textArea);
    }
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
                  className="btn-view-codes"
                  onClick={() => setShowClassCodesModal(true)}
                  type="button"
              >
                <BuildingIcon />
                Коды классов
              </button>
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
                                                {(() => {
                                                  const actionKey = `${classItem.id}:${student.id}`;
                                                  const isClosing = closingCourseKey === actionKey;
                                                  const isCourseClosed = Boolean(
                                                      student?.courseClosedAt
                                                  );
                                                  return (
                                                      <>
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
                                                        <div className="student-actions">
                                                          <button
                                                              className={`btn-close-course-student ${isCourseClosed ? "is-closed" : ""}`}
                                                              onClick={() =>
                                                                  handleCloseCourseForStudent(classItem.id, student.id, student.name)
                                                              }
                                                              type="button"
                                                              disabled={isClosing || isCourseClosed}
                                                          >
                                                            {isClosing
                                                                ? "Сохраняем..."
                                                                : isCourseClosed
                                                                    ? "Завершено"
                                                                    : "Завершить курс"}
                                                          </button>
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
                                                      </>
                                                  );
                                                })()}
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
              <div className="modal-overlay" onClick={() => {
                setShowAddStudentModal(false);
                setCreatedStudentMessage("");
              }}>
                <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                  <button
                      className="modal-close"
                      onClick={() => {
                        setShowAddStudentModal(false);
                        setCreatedStudentMessage("");
                      }}
                      type="button"
                      aria-label="Закрыть"
                  >
                    <XIcon />
                  </button>
                  <h2 className="modal-title">Создание ученика</h2>
                  <p className="modal-subtitle">
                    Создайте аккаунт ученика на платформе. После создания можно дать ему код класса для вступления.
                  </p>
                  {createdStudentMessage && (
                      <div className="class-management-success">{createdStudentMessage}</div>
                  )}

                  <form className="student-create-form" onSubmit={handleCreateStudent}>
                    <label className="student-create-label" htmlFor="student-name">
                      Имя
                    </label>
                    <input
                        id="student-name"
                        className="student-create-input"
                        type="text"
                        value={studentForm.name}
                        onChange={(e) => handleStudentFormChange("name", e.target.value)}
                        placeholder="Введите имя ученика"
                        required
                    />

                    <label className="student-create-label" htmlFor="student-email">
                      Email
                    </label>
                    <input
                        id="student-email"
                        className="student-create-input"
                        type="email"
                        value={studentForm.email}
                        onChange={(e) => handleStudentFormChange("email", e.target.value)}
                        placeholder="student@example.com"
                        required
                    />

                    <label className="student-create-label" htmlFor="student-password">
                      Пароль
                    </label>
                    <input
                        id="student-password"
                        className="student-create-input"
                        type="password"
                        value={studentForm.password}
                        onChange={(e) => handleStudentFormChange("password", e.target.value)}
                        placeholder="Минимум 6 символов"
                        minLength={6}
                        required
                    />

                    <label className="student-create-label" htmlFor="student-tgid">
                      Telegram ID (опционально)
                    </label>
                    <input
                        id="student-tgid"
                        className="student-create-input"
                        type="text"
                        value={studentForm.tgId}
                        onChange={(e) => handleStudentFormChange("tgId", e.target.value)}
                        placeholder="@username или id"
                    />

                    <div className="modal-actions">
                      <button
                          className="btn-secondary"
                          onClick={() => {
                            setShowAddStudentModal(false);
                            setCreatedStudentMessage("");
                          }}
                          type="button"
                          disabled={isCreatingStudent}
                      >
                        Закрыть
                      </button>
                      <button className="btn-accept" type="submit" disabled={isCreatingStudent}>
                        <CheckIcon />
                        {isCreatingStudent ? "Создание..." : "Создать ученика"}
                      </button>
                    </div>
                  </form>

                </div>
              </div>
          )}

          {showClassCodesModal && (
              <div className="modal-overlay" onClick={() => setShowClassCodesModal(false)}>
                <div className="modal-content modal-content-large" onClick={(e) => e.stopPropagation()}>
                  <button
                      className="modal-close"
                      onClick={() => setShowClassCodesModal(false)}
                      type="button"
                      aria-label="Закрыть"
                  >
                    <XIcon />
                  </button>
                  <h2 className="modal-title">Коды классов</h2>
                  <p className="modal-subtitle">
                    Коды для вступления в ваши классы. Поделитесь кодом с учениками, чтобы они могли подать заявку на вступление.
                  </p>

                  {classes.length === 0 ? (
                      <div className="class-codes-empty">
                        <BuildingIcon />
                        <p>У вас пока нет классов</p>
                      </div>
                  ) : (
                      <div className="class-codes-list">
                        {classes.map((classItem) => (
                            <div key={classItem.id} className="class-code-card">
                              <div className="class-code-header">
                                <div className="class-code-info">
                                  <h3 className="class-code-name">{classItem.name}</h3>
                                </div>
                              </div>
                              <div className="class-code-body">
                                <div className="class-code-label">Код для вступления:</div>
                                <div className="class-code-value-wrapper">
                                  <div className="class-code-value">
                                    {classItem.joinCode || "Не указан"}
                                  </div>
                                  <button
                                      className="btn-copy-code"
                                      onClick={() => handleCopyCode(classItem.joinCode)}
                                      type="button"
                                      title="Скопировать код"
                                  >
                                    {copiedCode === classItem.joinCode ? (
                                        <>
                                          <CheckIcon />
                                          Скопировано
                                        </>
                                    ) : (
                                        <>
                                          <PlusIcon />
                                          Копировать
                                        </>
                                    )}
                                  </button>
                                </div>
                              </div>
                            </div>
                        ))}
                      </div>
                  )}

                  <div className="modal-actions">
                    <button
                        className="btn-secondary"
                        onClick={() => setShowClassCodesModal(false)}
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
