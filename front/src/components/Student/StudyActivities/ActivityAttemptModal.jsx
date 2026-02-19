import { useEffect, useState } from "react";
import { startAttempt, submitAttempt, getActivityById } from "../../api/studentApi";
import "./ActivityAttemptModal.css";

const ACTIVITY_TYPE_LABELS = {
  HOMEWORK_TEST: "Домашняя работа",
  CONTROL_WORK: "Контрольная работа",
  WEEKLY_STAR: "Еженедельное задание",
  REMEDIAL_TASK: "Задача для отстающих",
};

function ActivityAttemptModal({ activity, onClose }) {
  const [attempt, setAttempt] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let isCancelled = false;

    const load = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        // Начинаем попытку
        const attemptData = await startAttempt(activity.id);
        if (!isCancelled) {
          setAttempt(attemptData);
        }

        // Загружаем активность с вопросами
        const activityData = await getActivityById(activity.id);
        if (!isCancelled && activityData.questions) {
          console.log("Loaded questions", activityData.questions);
          setQuestions(activityData.questions);
          // Инициализируем ответы для всех вопросов
          const initialAnswers = {};
          activityData.questions.forEach((q) => {
            const key = String(q.id);
            initialAnswers[key] = null; // null для инициализации
          });
          setAnswers(initialAnswers);
        }
      } catch (e) {
        if (!isCancelled) {
          setErrorMessage(e?.message || "Не удалось начать попытку");
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    load();
    return () => {
      isCancelled = true;
    };
  }, [activity.id]);

  const handleAnswerChange = (questionId, value) => {
    // Убеждаемся, что questionId - это число или строка
    const key = String(questionId);
    setAnswers((prev) => {
      const newAnswers = { ...prev, [key]: String(value) };
      return newAnswers;
    });
  };

  const handleSubmit = async () => {
    if (!attempt || questions.length === 0) {
      setErrorMessage("Нет вопросов для ответа");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage("");
    try {
      // Преобразуем ответы в правильный формат для всех вопросов
      const answerArray = questions.map((q) => {
        const answerKey = String(q.id);
        const answer = answers[answerKey];

        if (q.questionType === "SINGLE_CHOICE") {
          // Для SINGLE_CHOICE нужно найти индекс опции (1-4)
          if (!answer || answer === null || answer === undefined) {
            throw new Error(`Не выбран ответ для вопроса "${q.questionText || q.id}"`);
          }

          const options = [q.option1, q.option2, q.option3, q.option4].filter(Boolean);
          // Ищем точное совпадение текста опции
          const answerStr = String(answer).trim();
          const selectedIndex = options.findIndex(opt => String(opt).trim() === answerStr);

          if (selectedIndex < 0) {
            throw new Error(`Не выбран ответ для вопроса "${q.questionText || q.id}"`);
          }

          return {
            questionId: q.id,
            selectedOption: selectedIndex + 1, // 1-4
            textAnswer: null,
          };
        } else {
          // Для TEXT или OPEN
          const textAnswer = answer ? String(answer).trim() : "";
          if (!textAnswer) {
            throw new Error(`Не введен ответ для вопроса "${q.questionText || q.id}"`);
          }
          return {
            questionId: q.id,
            selectedOption: null,
            textAnswer: textAnswer,
          };
        }
      });

      await submitAttempt(attempt.id, answerArray);
      onClose();
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось отправить ответы");
    } finally {
      setIsSubmitting(false);
    }
  };

  const activityType = activity?.activityType || activity?.type;
  const activityTypeLabel = ACTIVITY_TYPE_LABELS[activityType] || "Активность";

  if (isLoading) {
    return (
        <div className="activity-attempt-modal-overlay">
          <div className="activity-attempt-modal">
            <div className="activity-attempt-modal-header">
              <h2>Загрузка...</h2>
            </div>
          </div>
        </div>
    );
  }

  if (!attempt) {
    return (
        <div className="activity-attempt-modal-overlay">
          <div className="activity-attempt-modal">
            <div className="activity-attempt-modal-header">
              <h2>{activity.title || "Активность"}</h2>
              <button
                  className="activity-attempt-modal-close"
                  onClick={onClose}
                  type="button"
              >
                ×
              </button>
            </div>
            {errorMessage && (
                <div className="activity-attempt-error">{errorMessage}</div>
            )}
          </div>
        </div>
    );
  }

  return (
      <div className="activity-attempt-modal-overlay">
        <div className="activity-attempt-modal">
          <div className="activity-attempt-modal-header">
            <h2>{activity.title || "Активность"}</h2>
            <button
                className="activity-attempt-modal-close"
                onClick={onClose}
                type="button"
            >
              ×
            </button>
          </div>
          <div className="activity-attempt-modal-description">
            {activityTypeLabel}
          </div>
          <div className="activity-attempt-modal-description">
            {activity.description || "Без описания"}
          </div>
          {errorMessage && (
              <div className="activity-attempt-error">{errorMessage}</div>
          )}
          <div className="activity-attempt-questions">
            {questions && questions.length > 0 ? (
                questions.map((question, idx) => {
                  const options = [question.option1, question.option2, question.option3, question.option4].filter(Boolean);
                  return (
                      <div key={question.id} className="activity-attempt-question">
                        <div className="activity-attempt-question-header">
                  <span className="activity-attempt-question-number">
                    Вопрос {idx + 1}
                  </span>
                          <span className="activity-attempt-question-points">
                    {question.points || 1} балл(ов)
                  </span>
                        </div>
                        <div className="activity-attempt-question-text">
                          {question.questionText || "Без текста"}
                        </div>
                        {question.questionType === "SINGLE_CHOICE" && options.length > 0 ? (
                            <div className="activity-attempt-options">
                              {options.map((option, optIdx) => {
                                if (!option) return null;
                                const optionValue = String(option).trim();
                                const questionKey = String(question.id);
                                const currentAnswer = answers[questionKey];
                                const isChecked = currentAnswer !== null && currentAnswer !== undefined && String(currentAnswer).trim() === optionValue;
                                return (
                                    <label
                                        key={`${question.id}-${optIdx}`}
                                        className="activity-attempt-option"
                                        style={{ cursor: "pointer", display: "flex", alignItems: "center", gap: "8px" }}
                                    >
                                      <input
                                          type="radio"
                                          name={`question-${question.id}`}
                                          value={optionValue}
                                          checked={isChecked}
                                          onChange={(e) => {
                                            const val = e.target.value;
                                            handleAnswerChange(question.id, val);
                                          }}
                                          onClick={(e) => {
                                            // Убеждаемся, что клик обрабатывается
                                            e.stopPropagation();
                                          }}
                                      />
                                      <span style={{ userSelect: "none", flex: 1 }}>{option}</span>
                                    </label>
                                );
                              })}
                            </div>
                        ) : (
                            <textarea
                                className="activity-attempt-text-answer"
                                value={answers[String(question.id)] || ""}
                                onChange={(e) => {
                                  handleAnswerChange(question.id, e.target.value);
                                }}
                                placeholder="Введите ваш ответ..."
                                rows={4}
                            />
                        )}
                      </div>
                  );
                })
            ) : (
                <div className="activity-attempt-empty">
                  Для этой активности пока не добавлены вопросы
                </div>
            )}
          </div>
          <div className="activity-attempt-modal-actions">
            <button
                className="activity-attempt-btn activity-attempt-btn-secondary"
                onClick={onClose}
                type="button"
                disabled={isSubmitting}
            >
              Отмена
            </button>
            <button
                className="activity-attempt-btn activity-attempt-btn-primary"
                onClick={handleSubmit}
                type="button"
                disabled={isSubmitting}
            >
              {isSubmitting ? "Отправка..." : "Отправить ответы"}
            </button>
          </div>
        </div>
      </div>
  );
}

export default ActivityAttemptModal;
