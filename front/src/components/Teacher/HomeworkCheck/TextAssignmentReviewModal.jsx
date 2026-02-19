import { useState, useEffect } from "react";
import "./HomeworkCheck.css";
import { CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { DocumentIcon } from "../../../svgs/ActivitySvg.jsx";

function TextAssignmentReviewModal({
                                     isOpen,
                                     onClose,
                                     attempt,
                                     activity,
                                     onSaveGrade,
                                   }) {
  const [grades, setGrades] = useState({});
  const [feedbacks, setFeedbacks] = useState({});
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  // Инициализируем оценки для OPEN вопросов
  useEffect(() => {
    if (attempt && activity) {
      const initialGrades = {};
      const initialFeedbacks = {};

      // Находим OPEN и TEXT вопросы
      const openQuestions = activity.questions?.filter(
          (q) => q.questionType === "OPEN" || q.questionType === "TEXT"
      ) || [];

      openQuestions.forEach((question) => {
        // Находим ответ студента на этот вопрос
        const answer = attempt.answers?.find(
            (a) => a.questionId === question.id
        );

        if (answer) {
          // Используем уже начисленные баллы, если есть, иначе 0
          initialGrades[question.id] = answer.pointsAwarded !== null && answer.pointsAwarded !== undefined
              ? answer.pointsAwarded
              : 0;
          initialFeedbacks[question.id] = answer.feedback || "";
        } else {
          initialGrades[question.id] = 0;
          initialFeedbacks[question.id] = "";
        }
      });

      setGrades(initialGrades);
      setFeedbacks(initialFeedbacks);
    }
  }, [attempt, activity]);

  const handleGradeChange = (questionId, value) => {
    const numValue = parseInt(value) || 0;
    const question = activity.questions?.find((q) => q.id === questionId);
    const maxPoints = question?.points || 1;

    if (numValue < 0) {
      setGrades((prev) => ({ ...prev, [questionId]: 0 }));
    } else if (numValue > maxPoints) {
      setGrades((prev) => ({ ...prev, [questionId]: maxPoints }));
    } else {
      setGrades((prev) => ({ ...prev, [questionId]: numValue }));
    }
  };

  const handleFeedbackChange = (questionId, value) => {
    setFeedbacks((prev) => ({ ...prev, [questionId]: value }));
  };

  const handleSave = async () => {
    setErrorMessage("");

    // Проверяем, что все OPEN вопросы оценены
    const openQuestions = activity.questions?.filter(
        (q) => q.questionType === "OPEN"
    ) || [];

    if (openQuestions.length === 0) {
      setErrorMessage("Нет открытых вопросов для оценки");
      return;
    }

    // Формируем данные для отправки
    const gradesData = openQuestions.map((question) => {
      const pointsAwarded = grades[question.id] !== undefined ? grades[question.id] : 0;
      const feedback = feedbacks[question.id] || "";

      return {
        questionId: question.id,
        pointsAwarded: pointsAwarded,
        feedback: feedback.trim() || null,
      };
    });

    setIsSaving(true);
    try {
      await onSaveGrade(attempt.id, gradesData);
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось сохранить оценку");
    } finally {
      setIsSaving(false);
    }
  };

  if (!isOpen || !attempt || !activity) return null;

  const openQuestions = activity.questions?.filter(
      (q) => q.questionType === "OPEN" || q.questionType === "TEXT"
  ) || [];

  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("ru-RU");
    } catch {
      return dateString;
    }
  };

  return (
      <div className="modal-overlay" onClick={onClose}>
        <div
            className="modal-content modal-content-large"
            onClick={(e) => e.stopPropagation()}
        >
          <button className="modal-close" onClick={onClose} type="button">
            <CloseIcon />
          </button>

          <div className="review-header">
            <h2 className="review-title">{activity.title || "Проверка работы"}</h2>
            <div className="review-meta">
              <span className="review-student">{attempt.studentName || "Студент"}</span>
              <span className="review-date">
              {formatDate(attempt.submittedAt)}
            </span>
              <span className="review-type-tag text">
              {openQuestions.some((q) => q.questionType === "OPEN") ? "Открытые вопросы" : "Текстовые вопросы"}
            </span>
            </div>
          </div>

          {errorMessage && (
              <div className="review-error">{errorMessage}</div>
          )}

          <div className="review-content">
            <div className="review-section">
              <h3 className="review-section-title">Ответы ученика</h3>
              <div className="questions-review-list">
                {openQuestions.map((question, qIndex) => {
                  const answer = attempt.answers?.find(
                      (a) => a.questionId === question.id
                  );
                  const studentAnswer = answer?.textAnswer || "Ответ не предоставлен";
                  const isOpen = question.questionType === "OPEN";
                  const maxPoints = question.points || 1;
                  const currentGrade = grades[question.id] !== undefined ? grades[question.id] : 0;
                  const currentFeedback = feedbacks[question.id] || "";

                  return (
                      <div key={question.id} className="question-review-item">
                        <div className="question-review-header">
                          <div className="question-review-number">
                            {question.orderIndex || qIndex + 1}.
                          </div>
                          <div className="question-review-text">
                            {question.questionText || "Вопрос без текста"}
                          </div>
                          <div className="question-review-points">
                            Макс. баллов: {maxPoints}
                          </div>
                        </div>

                        <div className="student-answer-block">
                          <label className="answer-label">Ответ ученика:</label>
                          <div className="answer-text">{studentAnswer}</div>
                        </div>

                        {isOpen && (
                            <div className="grade-feedback-section">
                              <div className="grade-input-group">
                                <label className="grade-label">
                                  Баллы (0-{maxPoints}):
                                </label>
                                <input
                                    type="number"
                                    className="grade-input"
                                    min="0"
                                    max={maxPoints}
                                    value={currentGrade}
                                    onChange={(e) =>
                                        handleGradeChange(question.id, e.target.value)
                                    }
                                />
                              </div>
                              <div className="feedback-input-group">
                                <label className="comment-label">
                                  Комментарий (опционально):
                                </label>
                                <textarea
                                    className="comment-textarea"
                                    placeholder="Введите комментарий к ответу..."
                                    value={currentFeedback}
                                    onChange={(e) =>
                                        handleFeedbackChange(question.id, e.target.value)
                                    }
                                    rows="3"
                                />
                              </div>
                            </div>
                        )}

                        {!isOpen && answer && (
                            <div className="auto-graded-info">
                              {answer.isCorrect ? (
                                  <span className="correct-badge">Правильно</span>
                              ) : (
                                  <span className="incorrect-badge">Неправильно</span>
                              )}
                              <span className="points-info">
                          Баллов: {answer.pointsAwarded || 0} / {maxPoints}
                        </span>
                            </div>
                        )}
                      </div>
                  );
                })}
              </div>
            </div>

            {openQuestions.some((q) => q.questionType === "OPEN") && (
                <div className="review-section">
                  <h3 className="review-section-title">Сохранить оценки</h3>
                  <button
                      className="btn-save-grade"
                      onClick={handleSave}
                      type="button"
                      disabled={isSaving}
                  >
                    <DocumentIcon />
                    {isSaving ? "Сохранение..." : "Сохранить оценки"}
                  </button>
                </div>
            )}
          </div>
        </div>
      </div>
  );
}

export default TextAssignmentReviewModal;
