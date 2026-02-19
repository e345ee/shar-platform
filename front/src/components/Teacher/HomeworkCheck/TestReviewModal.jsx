import { useState, useEffect } from "react";
import "./HomeworkCheck.css";
import { CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { CheckIcon, DocumentIcon } from "../../../svgs/ActivitySvg.jsx";

function TestReviewModal({
                           isOpen,
                           onClose,
                           attempt,
                           activity,
                           onSaveReview,
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

      // Находим OPEN вопросы
      const openQuestions = activity.questions?.filter(
          (q) => q.questionType === "OPEN"
      ) || [];

      openQuestions.forEach((question) => {
        const answer = attempt.answers?.find(
            (a) => a.questionId === question.id
        );

        if (answer) {
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

    // Находим OPEN вопросы
    const openQuestions = activity.questions?.filter(
        (q) => q.questionType === "OPEN"
    ) || [];

    if (openQuestions.length === 0) {
      // Если нет OPEN вопросов, просто закрываем модалку
      onClose();
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
      await onSaveReview(attempt.id, gradesData);
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось сохранить оценку");
    } finally {
      setIsSaving(false);
    }
  };

  if (!isOpen || !attempt || !activity) return null;

  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("ru-RU");
    } catch {
      return dateString;
    }
  };

  // Группируем вопросы по типам
  const singleChoiceQuestions = activity.questions?.filter(
      (q) => q.questionType === "SINGLE_CHOICE"
  ) || [];

  const openQuestions = activity.questions?.filter(
      (q) => q.questionType === "OPEN"
  ) || [];

  const textQuestions = activity.questions?.filter(
      (q) => q.questionType === "TEXT"
  ) || [];

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
            <h2 className="review-title">
              {activity.title || "Проверка теста"}
            </h2>
            <div className="review-meta">
              <span className="review-student">{attempt.studentName || "Студент"}</span>
              <span className="review-date">
              {formatDate(attempt.submittedAt)}
            </span>
              {attempt.score !== null && attempt.maxScore !== null && (
                  <span className="review-result">
                Результат: {attempt.score} / {attempt.maxScore}
              </span>
              )}
            </div>
          </div>

          {errorMessage && (
              <div className="review-error">{errorMessage}</div>
          )}

          <div className="review-content">
            <p className="review-subtitle">
              Просмотр ответов ученика и правильных ответов
            </p>

            {/* SINGLE_CHOICE вопросы */}
            {singleChoiceQuestions.length > 0 && (
                <div className="questions-review-list">
                  {singleChoiceQuestions.map((question, qIndex) => {
                    const answer = attempt.answers?.find(
                        (a) => a.questionId === question.id
                    );
                    const selectedOption = answer?.selectedOption || null;
                    const isCorrect = answer?.isCorrect || false;
                    const options = [
                      question.option1,
                      question.option2,
                      question.option3,
                      question.option4,
                    ].filter(Boolean);
                    const correctOption = question.correctOption || null;

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
                              {question.points || 1} балл(ов)
                            </div>
                          </div>

                          <div className="options-review-list">
                            {options.map((option, optIdx) => {
                              const optionNumber = optIdx + 1;
                              const isCorrectOption = correctOption === optionNumber;
                              const isStudentAnswer = selectedOption === optionNumber;
                              const isCorrectAndSelected =
                                  isCorrectOption && isStudentAnswer;
                              const isIncorrect = isStudentAnswer && !isCorrectOption;

                              return (
                                  <div
                                      key={optIdx}
                                      className={`option-review-item ${
                                          isCorrectAndSelected ? "correct" : ""
                                      } ${isIncorrect ? "incorrect" : ""} ${
                                          isCorrectOption && !isStudentAnswer
                                              ? "correct-not-selected"
                                              : ""
                                      }`}
                                  >
                                    <div className="option-review-radio">
                                      {isCorrectOption && <CheckIcon />}
                                    </div>
                                    <div className="option-review-text">
                              <span className="option-letter">
                                {String.fromCharCode(65 + optIdx)}.
                              </span>
                                      {option}
                                    </div>
                                    {isCorrectOption && (
                                        <span className="option-label correct-label">
                                Правильный
                              </span>
                                    )}
                                    {isStudentAnswer && (
                                        <span className="option-label student-label">
                                Ответ ученика
                              </span>
                                    )}
                                  </div>
                              );
                            })}
                          </div>

                          <div className="answer-result">
                            {isCorrect ? (
                                <span className="correct-badge">Правильно</span>
                            ) : (
                                <span className="incorrect-badge">Неправильно</span>
                            )}
                            <span className="points-info">
                        Баллов: {answer?.pointsAwarded || 0} / {question.points || 1}
                      </span>
                          </div>
                        </div>
                    );
                  })}
                </div>
            )}

            {/* TEXT вопросы */}
            {textQuestions.length > 0 && (
                <div className="questions-review-list">
                  {textQuestions.map((question, qIndex) => {
                    const answer = attempt.answers?.find(
                        (a) => a.questionId === question.id
                    );
                    const studentAnswer = answer?.textAnswer || "Ответ не предоставлен";
                    const isCorrect = answer?.isCorrect || false;

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
                              {question.points || 1} балл(ов)
                            </div>
                          </div>

                          <div className="student-answer-block">
                            <label className="answer-label">Ответ ученика:</label>
                            <div className="answer-text">{studentAnswer}</div>
                          </div>

                          <div className="answer-result">
                            {isCorrect ? (
                                <span className="correct-badge">Правильно</span>
                            ) : (
                                <span className="incorrect-badge">Неправильно</span>
                            )}
                            <span className="points-info">
                        Баллов: {answer?.pointsAwarded || 0} / {question.points || 1}
                      </span>
                          </div>
                        </div>
                    );
                  })}
                </div>
            )}

            {/* OPEN вопросы */}
            {openQuestions.length > 0 && (
                <div className="review-section">
                  <h3 className="review-section-title">Открытые вопросы для оценки</h3>
                  <div className="questions-review-list">
                    {openQuestions.map((question, qIndex) => {
                      const answer = attempt.answers?.find(
                          (a) => a.questionId === question.id
                      );
                      const studentAnswer = answer?.textAnswer || "Ответ не предоставлен";
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
                          </div>
                      );
                    })}
                  </div>
                </div>
            )}

            {/* Кнопка сохранения для OPEN вопросов */}
            {openQuestions.length > 0 && (
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

            {/* Если нет OPEN вопросов, показываем только кнопку закрытия */}
            {openQuestions.length === 0 && (
                <div className="review-section">
                  <p className="review-info">
                    Все вопросы проверены автоматически. Нет открытых вопросов для ручной проверки.
                  </p>
                  <button
                      className="btn-save-grade"
                      onClick={onClose}
                      type="button"
                  >
                    Закрыть
                  </button>
                </div>
            )}
          </div>
        </div>
      </div>
  );
}

export default TestReviewModal;
