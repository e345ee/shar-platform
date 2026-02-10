import { useState } from "react";
import "./HomeworkCheck.css";
import { CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { CheckIcon, DocumentIcon } from "../../../svgs/ActivitySvg.jsx";

function TestReviewModal({
  isOpen,
  onClose,
  assignment,
  studentName,
  onSaveReview,
}) {
  const [grade, setGrade] = useState(assignment.grade || 4);
  const [comment, setComment] = useState("");
  const [questions] = useState([
    {
      id: 1,
      text: "Какое уравнение является квадратным?",
      options: [
        { id: 1, text: "x + 2 = 0" },
        {
          id: 2,
          text: "x² + 3x + 2 = 0",
          isCorrect: true,
          isStudentAnswer: false,
        },
        { id: 3, text: "2x + 3 = 0" },
      ],
    },
    {
      id: 2,
      text: "Как найти дискриминант квадратного уравнения аx² + bx + c = 0?",
      options: [
        { id: 1, text: "b² - 4ac", isCorrect: true, isStudentAnswer: false },
        { id: 2, text: "a² + b² + c²", isStudentAnswer: true },
        { id: 3, text: "2ab + 3c" },
      ],
    },
  ]);

  const handleSave = () => {
    if (grade >= 2 && grade <= 5) {
      onSaveReview(assignment.id, grade, comment);
    }
  };

  if (!isOpen) return null;

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
            Вопросы для теста: {assignment.title}
          </h2>
          <div className="review-meta">
            <span className="review-student">{studentName}</span>
            <span className="review-date">{assignment.date}</span>
            {assignment.result && (
              <span className="review-result">
                Результат: {assignment.result}
              </span>
            )}
          </div>
        </div>

        <div className="review-content">
          <p className="review-subtitle">
            Просмотр ответов ученика и правильных ответов
          </p>

          <div className="questions-review-list">
            {questions.map((question, qIndex) => (
              <div key={question.id} className="question-review-item">
                <div className="question-review-header">
                  <div className="question-review-number">{qIndex + 1}.</div>
                  <div className="question-review-text">{question.text}</div>
                </div>

                <div className="options-review-list">
                  {question.options.map((option, oIndex) => {
                    const isCorrect = option.isCorrect;
                    const isStudentAnswer = option.isStudentAnswer;
                    const isCorrectAndSelected = isCorrect && isStudentAnswer;
                    const isIncorrect = isStudentAnswer && !isCorrect;

                    return (
                      <div
                        key={option.id}
                        className={`option-review-item ${
                          isCorrectAndSelected ? "correct" : ""
                        } ${isIncorrect ? "incorrect" : ""} ${
                          isCorrect && !isStudentAnswer
                            ? "correct-not-selected"
                            : ""
                        }`}
                      >
                        <div className="option-review-radio">
                          {isCorrect && <CheckIcon />}
                        </div>
                        <div className="option-review-text">
                          <span className="option-letter">
                            {String.fromCharCode(65 + oIndex)}.
                          </span>
                          {option.text}
                        </div>
                        {isCorrect && (
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
              </div>
            ))}
          </div>

          <div className="review-section">
            <h3 className="review-section-title">Оценка и комментарий</h3>
            <div className="grade-section">
              <label className="grade-label">Оценка (2-5) *</label>
              <div className="grade-input-wrapper">
                <input
                  type="number"
                  className="grade-input"
                  min="2"
                  max="5"
                  value={grade}
                  onChange={(e) => setGrade(parseInt(e.target.value) || 2)}
                />
              </div>
            </div>
            <div className="comment-section">
              <label className="comment-label">Комментарий (опционально)</label>
              <textarea
                className="comment-textarea"
                placeholder="Введите комментарий к работе..."
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                rows="4"
              />
            </div>
            <button
              className="btn-save-grade"
              onClick={handleSave}
              type="button"
            >
              <DocumentIcon />
              Сохранить оценку
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default TestReviewModal;
