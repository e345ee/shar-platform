import { useState } from "react";
import "./HomeworkCheck.css";
import { CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { DocumentIcon } from "../../../svgs/ActivitySvg.jsx";

function TextAssignmentReviewModal({
  isOpen,
  onClose,
  assignment,
  studentName,
  onSaveGrade,
}) {
  const [grade, setGrade] = useState(assignment.grade || 4);
  const [comment, setComment] = useState("");

  const studentAnswer = `Творчество А.С. Пушкина является одним из величайших достижений русской литературы. Основные темы и мотивы его произведений отражают глубокое понимание человеческой природы и общественных проблем.

В творчестве Пушкина центральное место занимают темы свободы, любви и дружбы. Поэт мастерски изображает внутренний мир своих героев, показывая их переживания и стремления. Особенно ярко это проявляется в романе "Евгений Онегин", где Пушкин создал глубокие психологические портреты персонажей.

Пушкин также обращается к теме патриотизма и исторического прошлого России, что особенно заметно в произведениях "Полтава" и "Медный всадник". Его творчество отличается реализмом и глубоким психологизмом, что делает его произведения актуальными и в наши дни.`;

  const handleSave = () => {
    if (grade >= 2 && grade <= 5) {
      onSaveGrade(assignment.id, grade, comment);
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
          <h2 className="review-title">{assignment.title}</h2>
          <div className="review-meta">
            <span className="review-student">{studentName}</span>
            <span className="review-date">{assignment.date}</span>
            <span className="review-type-tag text">Текст</span>
          </div>
        </div>

        <div className="review-content">
          <div className="review-section">
            <h3 className="review-section-title">Ответы ученика</h3>
            <div className="student-answer-block">
              <div className="task-question">
                <span className="task-number">1.</span>
                <span>
                  Напишите сочинение на тему "Основные темы и мотивы в
                  творчестве А.С. Пушкина"
                </span>
              </div>
              <div className="student-answer">
                <label className="answer-label">Ответ ученика:</label>
                <div className="answer-text">{studentAnswer}</div>
              </div>
            </div>
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

export default TextAssignmentReviewModal;
