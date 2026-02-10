import { useState } from "react";
import "./HomeworkCheck.css";
import { HomeIcon } from "../../../svgs/TeacherSvg.jsx";
import { ClassManagementIcon } from "../../../svgs/TeacherSvg.jsx";
import { CalendarIcon, DocumentIcon } from "../../../svgs/ActivitySvg.jsx";
import TextAssignmentReviewModal from "./TextAssignmentReviewModal";
import TestReviewModal from "./TestReviewModal";

function HomeworkCheck({ onBackToMain }) {
  const [selectedClass, setSelectedClass] = useState("9A");
  const [selectedStudent, setSelectedStudent] = useState("Иванов Петр");
  const [showTextModal, setShowTextModal] = useState(false);
  const [showTestModal, setShowTestModal] = useState(false);
  const [selectedAssignment, setSelectedAssignment] = useState(null);

  const classes = [
    {
      id: "9A",
      name: "9A",
      studentsCount: 2,
      ungradedCount: 1,
    },
    {
      id: "10Б",
      name: "10Б",
      studentsCount: 1,
      ungradedCount: 1,
    },
  ];

  const students = {
    "9A": [
      {
        id: "ivanov",
        name: "Иванов Петр",
        assignmentsCount: 2,
        ungradedCount: 1,
        avatar: "ИП",
      },
      {
        id: "smirnova",
        name: "Смирнова Анна",
        assignmentsCount: 1,
        ungradedCount: 0,
        avatar: "СА",
      },
    ],
    "10Б": [
      {
        id: "petrov",
        name: "Петров Иван",
        assignmentsCount: 1,
        ungradedCount: 0,
        avatar: "ПИ",
      },
    ],
  };

  const assignments = {
    ivanov: [
      {
        id: 1,
        title: "Контрольная работа по алгебре",
        type: "Тест",
        date: "03.02.2024",
        status: "pending",
        result: "2 из 2 правильных",
      },
      {
        id: 2,
        title: "Сочинение: Творчество Пушкина",
        type: "Текст",
        date: "02.02.2024",
        status: "graded",
        grade: 5,
      },
    ],
    smirnova: [
      {
        id: 3,
        title: "Домашнее задание по геометрии",
        type: "Текст",
        date: "01.02.2024",
        status: "graded",
        grade: 4,
      },
    ],
    petrov: [
      {
        id: 4,
        title: "Тест по физике",
        type: "Тест",
        date: "31.01.2024",
        status: "graded",
        result: "8 из 10 правильных",
      },
    ],
  };

  const currentStudents = students[selectedClass] || [];
  const currentStudentId = currentStudents.find(
    (s) => s.name === selectedStudent,
  )?.id;
  const currentAssignments = assignments[currentStudentId] || [];

  const handleOpenAssignment = (assignment) => {
    setSelectedAssignment(assignment);
    if (assignment.type === "Текст") {
      setShowTextModal(true);
    } else {
      setShowTestModal(true);
    }
  };

  const handleSaveGrade = (assignmentId, grade, comment) => {
    // Здесь можно добавить логику сохранения оценки
    console.log("Saving grade:", assignmentId, grade, comment);
    setShowTextModal(false);
    setSelectedAssignment(null);
  };

  const handleSaveTestReview = (assignmentId, grade, comment) => {
    // Здесь можно добавить логику сохранения проверки теста
    console.log("Saving test review:", assignmentId, grade, comment);
    setShowTestModal(false);
    setSelectedAssignment(null);
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
              {classes.map((classItem) => (
                <div
                  key={classItem.id}
                  className={`class-card ${selectedClass === classItem.id ? "selected" : ""}`}
                  onClick={() => {
                    setSelectedClass(classItem.id);
                    setSelectedStudent(null);
                  }}
                >
                  <div className="class-name">{classItem.name}</div>
                  <div className="class-info">
                    <span>{classItem.studentsCount} учеников</span>
                  </div>
                  {classItem.ungradedCount > 0 && (
                    <div className="class-badge ungraded">
                      {classItem.ungradedCount} непроверенных
                    </div>
                  )}
                </div>
              ))}
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
                <p className="column-subtitle">Класс {selectedClass}</p>
              </div>
            </div>
            <div className="column-content">
              {currentStudents.map((student) => (
                <div
                  key={student.id}
                  className={`student-card ${selectedStudent === student.name ? "selected" : ""}`}
                  onClick={() => setSelectedStudent(student.name)}
                >
                  <div
                    className="student-avatar"
                    style={{
                      background:
                        selectedStudent === student.name
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
              ))}
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
                  {selectedStudent || "Выберите ученика"}
                </p>
              </div>
            </div>
            <div className="column-content">
              {selectedStudent && currentAssignments.length > 0 ? (
                currentAssignments.map((assignment) => (
                  <div
                    key={assignment.id}
                    className="assignment-card"
                    onClick={() => handleOpenAssignment(assignment)}
                  >
                    <div className="assignment-header">
                      <h3 className="assignment-title">{assignment.title}</h3>
                      <span
                        className={`assignment-type-tag ${assignment.type === "Тест" ? "test" : "text"}`}
                      >
                        {assignment.type}
                      </span>
                    </div>
                    <div className="assignment-details">
                      <div className="assignment-date">
                        <CalendarIcon />
                        <span>{assignment.date}</span>
                      </div>
                      {assignment.status === "pending" && (
                        <div className="assignment-status pending">
                          <DocumentIcon />
                          Ожидает проверки
                        </div>
                      )}
                      {assignment.status === "graded" &&
                        assignment.type === "Тест" && (
                          <div className="assignment-result">
                            Результат: {assignment.result}
                          </div>
                        )}
                      {assignment.status === "graded" &&
                        assignment.type === "Текст" && (
                          <div className="assignment-grade">
                            Оценка: {assignment.grade}
                          </div>
                        )}
                    </div>
                  </div>
                ))
              ) : (
                <div className="empty-state">
                  {selectedStudent
                    ? "Нет работ для отображения"
                    : "Выберите ученика для просмотра работ"}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {selectedAssignment && selectedAssignment.type === "Текст" && (
        <TextAssignmentReviewModal
          isOpen={showTextModal}
          onClose={() => {
            setShowTextModal(false);
            setSelectedAssignment(null);
          }}
          assignment={selectedAssignment}
          studentName={selectedStudent}
          onSaveGrade={handleSaveGrade}
        />
      )}

      {selectedAssignment && selectedAssignment.type === "Тест" && (
        <TestReviewModal
          isOpen={showTestModal}
          onClose={() => {
            setShowTestModal(false);
            setSelectedAssignment(null);
          }}
          assignment={selectedAssignment}
          studentName={selectedStudent}
          onSaveReview={handleSaveTestReview}
        />
      )}
    </div>
  );
}

export default HomeworkCheck;
