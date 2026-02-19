import { useEffect, useMemo, useState } from "react";
import { getActivityById, getAttempt, listMyAttempts } from "../../api/studentApi";
import "./MyAttemptsModal.css";

const STATUS_LABELS = {
    IN_PROGRESS: "В процессе",
    SUBMITTED: "Сдано",
    GRADED: "Проверено",
};

function formatDate(value) {
    if (!value) return "—";
    try {
        const d = new Date(value);
        if (Number.isNaN(d.getTime())) return String(value);
        return d.toLocaleString("ru-RU");
    } catch {
        return String(value);
    }
}

function MyAttemptsModal({ activity, onClose }) {
    const [summaries, setSummaries] = useState([]);
    const [selectedAttemptId, setSelectedAttemptId] = useState(null);
    const [attemptDetails, setAttemptDetails] = useState(null);
    const [isLoadingSummaries, setIsLoadingSummaries] = useState(false);
    const [isLoadingAttempt, setIsLoadingAttempt] = useState(false);
    const [isLoadingActivity, setIsLoadingActivity] = useState(false);
    const [activityDetails, setActivityDetails] = useState(null);
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        let isCancelled = false;

        const loadSummaries = async () => {
            if (!activity?.id) return;
            setIsLoadingSummaries(true);
            setErrorMessage("");
            setSummaries([]);
            setAttemptDetails(null);
            try {
                const data = await listMyAttempts(activity.id);
                if (isCancelled) return;
                const sorted = [...(Array.isArray(data) ? data : [])].sort(
                    (a, b) => Number(b?.attemptNumber || 0) - Number(a?.attemptNumber || 0),
                );
                setSummaries(sorted);
                setSelectedAttemptId(sorted[0]?.id || null);
            } catch (e) {
                if (!isCancelled) {
                    setErrorMessage(e?.message || "Не удалось загрузить попытки");
                }
            } finally {
                if (!isCancelled) {
                    setIsLoadingSummaries(false);
                }
            }
        };

        loadSummaries();
        return () => {
            isCancelled = true;
        };
    }, [activity?.id]);

    useEffect(() => {
        let isCancelled = false;
        const loadActivity = async () => {
            if (!activity?.id) return;
            setIsLoadingActivity(true);
            try {
                const details = await getActivityById(activity.id);
                if (!isCancelled) {
                    setActivityDetails(details);
                }
            } catch (e) {
                if (!isCancelled) {
                    setActivityDetails(null);
                }
            } finally {
                if (!isCancelled) {
                    setIsLoadingActivity(false);
                }
            }
        };
        loadActivity();
        return () => {
            isCancelled = true;
        };
    }, [activity?.id]);

    useEffect(() => {
        if (!selectedAttemptId) {
            setAttemptDetails(null);
            return;
        }
        let isCancelled = false;

        const loadAttempt = async () => {
            setIsLoadingAttempt(true);
            setErrorMessage("");
            try {
                const details = await getAttempt(selectedAttemptId);
                if (!isCancelled) {
                    setAttemptDetails(details);
                }
            } catch (e) {
                if (!isCancelled) {
                    setErrorMessage(e?.message || "Не удалось загрузить разбор попытки");
                    setAttemptDetails(null);
                }
            } finally {
                if (!isCancelled) {
                    setIsLoadingAttempt(false);
                }
            }
        };

        loadAttempt();
        return () => {
            isCancelled = true;
        };
    }, [selectedAttemptId]);

    const answers = useMemo(() => {
        const rows = Array.isArray(attemptDetails?.answers) ? attemptDetails.answers : [];
        return [...rows].sort(
            (a, b) => Number(a?.questionOrderIndex || 0) - Number(b?.questionOrderIndex || 0),
        );
    }, [attemptDetails]);

    const questionById = useMemo(() => {
        const questions = Array.isArray(activityDetails?.questions) ? activityDetails.questions : [];
        const map = new Map();
        questions.forEach((q) => {
            if (q?.id != null) {
                map.set(Number(q.id), q);
            }
        });
        return map;
    }, [activityDetails]);

    if (!activity) return null;

    return (
        <div className="my-attempts-overlay" onClick={onClose}>
            <div className="my-attempts-modal" onClick={(e) => e.stopPropagation()}>
                <div className="my-attempts-header">
                    <div>
                        <h2>{activity.title || "Мои попытки"}</h2>
                        <p>Разбор ответов и правильных решений</p>
                    </div>
                    <button type="button" onClick={onClose} className="my-attempts-close">
                        ×
                    </button>
                </div>

                {errorMessage && <div className="my-attempts-error">{errorMessage}</div>}

                <div className="my-attempts-body">
                    <aside className="my-attempts-list">
                        <h3>Попытки</h3>
                        {isLoadingSummaries ? (
                            <div className="my-attempts-empty">Загрузка попыток...</div>
                        ) : summaries.length > 0 ? (
                            summaries.map((item) => (
                                <button
                                    key={item.id}
                                    type="button"
                                    className={`my-attempt-item ${
                                        Number(selectedAttemptId) === Number(item.id) ? "active" : ""
                                    }`}
                                    onClick={() => setSelectedAttemptId(item.id)}
                                >
                                    <strong>Попытка #{item.attemptNumber || item.id}</strong>
                                    <span>{STATUS_LABELS[item.status] || item.status || "—"}</span>
                                    <span>
                    {item.score != null && item.maxScore != null
                        ? `${item.score}/${item.maxScore}`
                        : "—"}
                  </span>
                                </button>
                            ))
                        ) : (
                            <div className="my-attempts-empty">Пока нет попыток по этой активности</div>
                        )}
                    </aside>

                    <section className="my-attempts-details">
                        {isLoadingAttempt || isLoadingActivity ? (
                            <div className="my-attempts-empty">Загрузка разбора...</div>
                        ) : attemptDetails ? (
                            <>
                                <div className="my-attempt-summary">
                      <span>
                        Статус: {STATUS_LABELS[attemptDetails.status] || attemptDetails.status || "—"}
                      </span>
                                    <span>Сдано: {formatDate(attemptDetails.submittedAt)}</span>
                                    <span>
                        Баллы:{" "}
                                        {attemptDetails.score != null && attemptDetails.maxScore != null
                                            ? `${attemptDetails.score}/${attemptDetails.maxScore}`
                                            : "—"}
                      </span>
                                </div>

                                {answers.length > 0 ? (
                                    <div className="my-attempt-answers">
                                        {answers.map((answer, idx) => {
                                            const question = questionById.get(Number(answer?.questionId)) || null;
                                            const type = String(question?.questionType || answer?.questionType || "");
                                            const options = [
                                                question?.option1 || answer?.option1,
                                                question?.option2 || answer?.option2,
                                                question?.option3 || answer?.option3,
                                                question?.option4 || answer?.option4,
                                            ];
                                            const correctOption = question?.correctOption || answer?.correctOption;
                                            const selectedOption = answer?.selectedOption;
                                            const isCorrect = answer?.isCorrect;
                                            return (
                                                <div key={answer?.id || `${idx}`} className="my-attempt-question">
                                                    <h4>
                                                        Вопрос {question?.orderIndex || answer?.questionOrderIndex || idx + 1}
                                                    </h4>
                                                    <p>{question?.questionText || answer?.questionText || "Без текста вопроса"}</p>

                                                    {type === "SINGLE_CHOICE" && (
                                                        <div className="my-attempt-options">
                                                            {options
                                                                .map((opt, optIdx) => ({ opt, optIdx }))
                                                                .filter((item) => Boolean(item.opt))
                                                                .map(({ opt, optIdx }) => {
                                                                    const optionNumber = optIdx + 1;
                                                                    const isStudent = selectedOption === optionNumber;
                                                                    const isRight = correctOption === optionNumber;
                                                                    return (
                                                                        <div
                                                                            key={`${answer?.id}-${optionNumber}`}
                                                                            className={`my-attempt-option ${
                                                                                isStudent ? "student" : ""
                                                                            } ${isRight ? "correct" : ""}`}
                                                                        >
                                                    <span className="my-attempt-option-letter">
                                                      {String.fromCharCode(65 + optIdx)}.
                                                    </span>
                                                                            <span className="my-attempt-option-text">{opt}</span>
                                                                            {isStudent && (
                                                                                <span className="my-attempt-option-mark student-mark">
                                                          Ваш ответ
                                                        </span>
                                                                            )}
                                                                            {isRight && (
                                                                                <span className="my-attempt-option-mark correct-mark">
                                                          Правильный
                                                        </span>
                                                                            )}
                                                                        </div>
                                                                    );
                                                                })}
                                                        </div>
                                                    )}

                                                    {(type === "TEXT" || type === "OPEN") && (
                                                        <div className="my-attempt-texts">
                                                            <div>
                                                                <strong>Ваш ответ:</strong>{" "}
                                                                {answer?.textAnswer || "Ответ не предоставлен"}
                                                            </div>
                                                            {type === "TEXT" && (
                                                                <div>
                                                                    <strong>Правильный ответ:</strong>{" "}
                                                                    {answer?.correctTextAnswer || "Недоступно"}
                                                                </div>
                                                            )}
                                                        </div>
                                                    )}

                                                    <div className="my-attempt-result">
                                    <span
                                        className={`my-attempt-result-status ${
                                            isCorrect == null
                                                ? "pending"
                                                : isCorrect
                                                    ? "correct"
                                                    : "incorrect"
                                        }`}
                                    >
                                      Результат:{" "}
                                        {isCorrect == null
                                            ? "Ожидает проверки"
                                            : isCorrect
                                                ? "Правильно"
                                                : "Есть ошибка"}
                                    </span>
                                                        <span className="my-attempt-result-points">
                                      Баллы:{" "}
                                                            {answer?.pointsAwarded != null ? answer.pointsAwarded : "—"}
                                    </span>
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                ) : (
                                    <div className="my-attempts-empty">В этой попытке нет сохраненных ответов</div>
                                )}
                            </>
                        ) : (
                            <div className="my-attempts-empty">Выберите попытку для разбора</div>
                        )}
                    </section>
                </div>
            </div>
        </div>
    );
}

export default MyAttemptsModal;
