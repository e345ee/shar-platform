import { useState, useEffect } from "react";
import "./HomeworkCheck.css";
import { HomeIcon } from "../../../svgs/TeacherSvg.jsx";
import { ClassManagementIcon } from "../../../svgs/TeacherSvg.jsx";
import { CalendarIcon, DocumentIcon } from "../../../svgs/ActivitySvg.jsx";
import TextAssignmentReviewModal from "./TextAssignmentReviewModal";
import TestReviewModal from "./TestReviewModal";
import {
  listMyClasses,
  listClassStudents,
  listPendingAttempts,
  getAttempt,
  getActivityById,
  gradeAttempt,
} from "../../api/teacherApi";

function HomeworkCheck({ onBackToMain }) {
  const [selectedClassId, setSelectedClassId] = useState(null);
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  const [classes, setClasses] = useState([]);
  const [students, setStudents] = useState([]);
  const [attempts, setAttempts] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [showTextModal, setShowTextModal] = useState(false);
  const [showTestModal, setShowTestModal] = useState(false);
  const [selectedAttempt, setSelectedAttempt] = useState(null);
  const [selectedActivity, setSelectedActivity] = useState(null);

  // Загружаем классы преподавателя
  useEffect(() => {
    let isCancelled = false;

    const loadClasses = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const classesData = await listMyClasses();
        if (!isCancelled) {
          setClasses(classesData);
          if (classesData.length > 0 && !selectedClassId) {
            setSelectedClassId(classesData[0].id);
          }
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось загрузить классы");
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    loadClasses();
    return () => {
      isCancelled = true;
    };
  }, []);

  // Загружаем студентов выбранного класса
  useEffect(() => {
    if (!selectedClassId) {
      setStudents([]);
      return;
    }

    let isCancelled = false;

    const loadStudents = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const studentsData = await listClassStudents(selectedClassId, 0, 100);
        if (!isCancelled) {
          setStudents(studentsData.content || []);
          setSelectedStudentId(null);
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось загрузить студентов");
          setStudents([]);
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    loadStudents();
    return () => {
      isCancelled = true;
    };
  }, [selectedClassId]);

  // Загружаем попытки, требующие проверки
  useEffect(() => {
    let isCancelled = false;

    const loadAttempts = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const attemptsData = await listPendingAttempts({
          classId: selectedClassId,
          page: 0,
          size: 100,
        });
        if (!isCancelled) {
          setAttempts(attemptsData.content || []);
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось загрузить попытки");
          setAttempts([]);
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    loadAttempts();
    return () => {
      isCancelled = true;
    };
  }, [selectedClassId]);

  // Фильтруем попытки по выбранному студенту
  const filteredAttempts = selectedStudentId
      ? attempts.filter((a) => a.studentId === selectedStudentId)
      : attempts;

  // Группируем попытки по студентам
  const attemptsByStudent = {};
  filteredAttempts.forEach((attempt) => {
    const studentId = attempt.studentId;
    if (!attemptsByStudent[studentId]) {
      attemptsByStudent[studentId] = [];
    }
    attemptsByStudent[studentId].push(attempt);
  });

  // Подсчитываем статистику для классов
  const classesWithStats = classes.map((cls) => {
    const classAttempts = attempts.filter((a) => a.classId === cls.id);
    const ungradedCount = classAttempts.length;
    const classStudents = students.filter((s) => {
      const studentAttempts = attempts.filter((a) => a.studentId === s.id);
      return studentAttempts.length > 0;
    });
    return {
      ...cls,
      studentsCount: classStudents.length,
      ungradedCount,
    };
  });

  // Подсчитываем статистику для студентов
  const studentsWithStats = students.map((student) => {
    const studentAttempts = attempts.filter((a) => a.studentId === student.id);
    return {
      ...student,
      assignmentsCount: studentAttempts.length,
      ungradedCount: studentAttempts.length,
      avatar: student.name
          ? student.name
              .split(" ")
              .map((n) => n[0])
              .join("")
              .toUpperCase()
              .slice(0, 2)
          : "??",
    };
  });

  const handleOpenAttempt = async (attempt) => {
    setErrorMessage("");
    try {
      setIsLoading(true);
      // Загружаем попытку с ответами
      const attemptData = await getAttempt(attempt.attemptId);
      // Загружаем активность с вопросами
      const activityData = await getActivityById(attempt.testId);

      setSelectedAttempt(attemptData);
      setSelectedActivity(activityData);

      // Определяем тип активности по наличию OPEN вопросов
      const hasOpenQuestions = activityData.questions?.some(
          (q) => q.questionType === "OPEN"
      );
      const hasTextQuestions = activityData.questions?.some(
          (q) => q.questionType === "TEXT"
      );

      if (hasOpenQuestions || hasTextQuestions) {
        setShowTextModal(true);
      } else {
        setShowTestModal(true);
      }
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось загрузить попытку");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveGrade = async (attemptId, grades) => {
    setErrorMessage("");
    try {
      setIsLoading(true);
      await gradeAttempt(attemptId, { grades });
      // Обновляем список попыток
      const attemptsData = await listPendingAttempts({
        classId: selectedClassId,
        page: 0,
        size: 100,
      });
      setAttempts(attemptsData.content || []);
      setShowTextModal(false);
      setShowTestModal(false);
      setSelectedAttempt(null);
      setSelectedActivity(null);
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось сохранить оценку");
    } finally {
      setIsLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("ru-RU");
    } catch {
      return dateString;
    }
  };

  const getActivityTypeLabel = (type) => {
    const labels = {
      HOMEWORK_TEST: "Домашнее задание (тест)",
      CONTROL_WORK: "Контрольная работа",
      WEEKLY_STAR: "Еженедельное задание",
      REMEDIAL_TASK: "Задача для отстающих",
    };
    return labels[type] || type;
  };

  return (
      <div className="homework-check">
        <div className="homework-check-container">
          <header className="homework-check-header">
            <h1 className="homework-check-title">Проверка домашних заданий</h1>
            <button className="btn-home" onClick={onBackToMain} type="button">
              <HomeIcon />
              На главную
            </button>
          </header>

          {errorMessage && (
              <div className="homework-check-error">{errorMessage}</div>
          )}

          <div className="homework-check-content">
            {/* Классы */}
            <div className="homework-column">
              <div className="column-header">
                <div className="column-header-icon">
                  <ClassManagementIcon />
                </div>
                <div>
                  <h2 className="column-title">Классы</h2>
                  <p className="column-subtitle">
                    Выберите класс для просмотра учеников
                  </p>
                </div>
              </div>
              <div className="column-content">
                {isLoading ? (
                    <div className="empty-state">Загрузка...</div>
                ) : classesWithStats.length > 0 ? (
                    classesWithStats.map((classItem) => (
                        <div
                            key={classItem.id}
                            className={`class-card ${
                                selectedClassId === classItem.id ? "selected" : ""
                            }`}
                            onClick={() => {
                              setSelectedClassId(classItem.id);
                              setSelectedStudentId(null);
                            }}
                        >
                          <div className="class-name">{classItem.name}</div>
                          <div className="class-info">
                            <span>{classItem.studentsCount || 0} учеников</span>
                          </div>
                          {classItem.ungradedCount > 0 && (
                              <div className="class-badge ungraded">
                                {classItem.ungradedCount} непроверенных
                              </div>
                          )}
                        </div>
                    ))
                ) : (
                    <div className="empty-state">Нет доступных классов</div>
                )}
              </div>
            </div>

            {/* Ученики */}
            <div className="homework-column">
              <div className="column-header">
                <div className="column-header-icon">
                  <ClassManagementIcon />
                </div>
                <div>
                  <h2 className="column-title">Ученики</h2>
                  <p className="column-subtitle">
                    {selectedClassId
                        ? classes.find((c) => c.id === selectedClassId)?.name ||
                        "Выберите класс"
                        : "Выберите класс"}
                  </p>
                </div>
              </div>
              <div className="column-content">
                {isLoading ? (
                    <div className="empty-state">Загрузка...</div>
                ) : selectedClassId && studentsWithStats.length > 0 ? (
                    studentsWithStats.map((student) => (
                        <div
                            key={student.id}
                            className={`student-card ${
                                selectedStudentId === student.id ? "selected" : ""
                            }`}
                            onClick={() => setSelectedStudentId(student.id)}
                        >
                          <div
                              className="student-avatar"
                              style={{
                                background:
                                    selectedStudentId === student.id
                                        ? "linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%)"
                                        : "#e5e7eb",
                              }}
                          >
                            {student.avatar}
                          </div>
                          <div className="student-info">
                            <div className="student-name">{student.name}</div>
                            <div className="student-assignments">
                              {student.assignmentsCount} работ
                            </div>
                          </div>
                          {student.ungradedCount > 0 && (
                              <div className="student-badge">{student.ungradedCount}</div>
                          )}
                        </div>
                    ))
                ) : selectedClassId ? (
                    <div className="empty-state">Нет учеников в классе</div>
                ) : (
                    <div className="empty-state">Выберите класс</div>
                )}
              </div>
            </div>

            {/* Работы */}
            <div className="homework-column">
              <div className="column-header">
                <div className="column-header-icon">
                  <DocumentIcon />
                </div>
                <div>
                  <h2 className="column-title">Работы</h2>
                  <p className="column-subtitle">
                    {selectedStudentId
                        ? students.find((s) => s.id === selectedStudentId)?.name ||
                        "Выберите ученика"
                        : "Выберите ученика"}
                  </p>
                </div>
              </div>
              <div className="column-content">
                {isLoading ? (
                    <div className="empty-state">Загрузка...</div>
                ) : selectedStudentId && filteredAttempts.length > 0 ? (
                    filteredAttempts.map((attempt) => {
                      const student = students.find(
                          (s) => s.id === attempt.studentId
                      );
                      return (
                          <div
                              key={attempt.attemptId}
                              className="assignment-card"
                              onClick={() => handleOpenAttempt(attempt)}
                          >
                            <div className="assignment-header">
                              <h3 className="assignment-title">
                                {attempt.activityTitle || `Активность #${attempt.testId || attempt.attemptId}`}
                              </h3>
                              <span className="assignment-type-tag test">
                          {attempt.activityType ? getActivityTypeLabel(attempt.activityType) : "Тест"}
                        </span>
                            </div>
                            <div className="assignment-details">
                              <div className="assignment-date">
                                <CalendarIcon />
                                <span>
                            {formatDate(attempt.submittedAt) || "Дата неизвестна"}
                          </span>
                              </div>
                              <div className="assignment-status pending">
                                <DocumentIcon />
                                Ожидает проверки
                              </div>
                              {attempt.ungradedOpenCount > 0 && (
                                  <div className="assignment-info">
                                    {attempt.ungradedOpenCount} открытых вопросов
                                  </div>
                              )}
                            </div>
                          </div>
                      );
                    })
                ) : selectedStudentId ? (
                    <div className="empty-state">Нет работ для проверки</div>
                ) : (
                    <div className="empty-state">Выберите ученика</div>
                )}
              </div>
            </div>
          </div>
        </div>

        {selectedAttempt && selectedActivity && (
            <>
              {showTextModal && (
                  <TextAssignmentReviewModal
                      isOpen={showTextModal}
                      onClose={() => {
                        setShowTextModal(false);
                        setSelectedAttempt(null);
                        setSelectedActivity(null);
                      }}
                      attempt={selectedAttempt}
                      activity={selectedActivity}
                      onSaveGrade={handleSaveGrade}
                  />
              )}

              {showTestModal && (
                  <TestReviewModal
                      isOpen={showTestModal}
                      onClose={() => {
                        setShowTestModal(false);
                        setSelectedAttempt(null);
                        setSelectedActivity(null);
                      }}
                      attempt={selectedAttempt}
                      activity={selectedActivity}
                      onSaveReview={handleSaveGrade}
                  />
              )}
            </>
        )}
      </div>
  );
}

export default HomeworkCheck;
