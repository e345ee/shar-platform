import { useState, useEffect } from "react";
import "./StudyActivity.css";
import {
    CloseIcon,
    PlusIcon,
    TrashIcon,
    RadioCheckedIcon,
    RadioUncheckedIcon,
} from "../../../svgs/MethodistSvg.jsx";

function TestModal({ isOpen, onClose, activity, onSaveQuestions }) {
    const [questions, setQuestions] = useState([]);
    const [localError, setLocalError] = useState("");
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        if (activity) {
            setQuestions(activity.questions || []);
            setLocalError("");
        }
    }, [activity]);

    const handleAddQuestion = () => {
        setLocalError("");
        setQuestions([
            ...questions,
            {
                id: Date.now(),
                text: "",
                questionType: "SINGLE_CHOICE",
                points: 1,
                options: [
                    { id: 1, text: "" },
                    { id: 2, text: "" },
                    { id: 3, text: "" },
                    { id: 4, text: "" },
                ],
                correctAnswer: null,
                correctTextAnswer: "",
            },
        ]);
    };

    const handleQuestionChange = (questionId, field, value) => {
        setQuestions(
            questions.map((question) =>
                question.id === questionId ? { ...question, [field]: value } : question
            )
        );
    };

    const handleOptionChange = (questionId, optionId, text) => {
        setQuestions(
            questions.map((question) =>
                question.id === questionId
                    ? {
                        ...question,
                        options: question.options.map((option) =>
                            option.id === optionId ? { ...option, text } : option
                        ),
                    }
                    : question
            )
        );
    };

    const handleCorrectAnswerChange = (questionId, optionId) => {
        setQuestions(
            questions.map((question) =>
                question.id === questionId
                    ? { ...question, correctAnswer: optionId }
                    : question
            )
        );
    };

    const handleDeleteQuestion = (questionId) => {
        setQuestions(questions.filter((question) => question.id !== questionId));
    };

    const handleSave = async () => {
        setLocalError("");
        setIsSaving(true);
        try {
            await onSaveQuestions(activity.id, questions);
            onClose();
        } catch (error) {
            setLocalError(error?.message || "Не удалось сохранить вопросы");
        } finally {
            setIsSaving(false);
        }
    };

    if (!isOpen || !activity) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content modal-content-large" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={onClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">Вопросы: {activity.title}</h2>
                <p className="modal-subtitle">
                    {activity.format} - Управление вопросами и заданиями
                </p>
                {localError && <div className="activity-error">{localError}</div>}

                <div className="questions-list">
                    {questions.map((question, index) => (
                        <div key={question.id} className="question-item">
                            <div className="question-header">
                                <div className="question-number">{index + 1}</div>
                                <button
                                    className="question-delete-btn"
                                    onClick={() => handleDeleteQuestion(question.id)}
                                    type="button"
                                    aria-label="Delete question"
                                >
                                    <TrashIcon />
                                </button>
                            </div>
                            <textarea
                                className="question-text-input"
                                placeholder="Введите текст вопроса..."
                                value={question.text}
                                onChange={(e) =>
                                    handleQuestionChange(question.id, "text", e.target.value)
                                }
                                rows="2"
                            />
                            <div className="question-meta-row">
                                <select
                                    className="modal-select question-type-select"
                                    value={question.questionType || "SINGLE_CHOICE"}
                                    onChange={(e) =>
                                        handleQuestionChange(question.id, "questionType", e.target.value)
                                    }
                                >
                                    <option value="SINGLE_CHOICE">Тест (один вариант)</option>
                                    <option value="TEXT">Текстовый ответ</option>
                                    <option value="OPEN">Открытый ответ</option>
                                </select>
                                <input
                                    type="number"
                                    min="1"
                                    className="modal-input question-points-input"
                                    value={question.points || 1}
                                    onChange={(e) => handleQuestionChange(question.id, "points", e.target.value)}
                                    placeholder="Баллы"
                                />
                            </div>
                            {(question.questionType || "SINGLE_CHOICE") === "SINGLE_CHOICE" && (
                                <div className="options-list">
                                    {question.options.map((option, optIndex) => (
                                        <div
                                            key={option.id}
                                            className={`option-item ${
                                                question.correctAnswer === option.id
                                                    ? "option-correct"
                                                    : ""
                                            }`}
                                        >
                                            <button
                                                className="option-radio"
                                                onClick={() =>
                                                    handleCorrectAnswerChange(question.id, option.id)
                                                }
                                                type="button"
                                            >
                                                {question.correctAnswer === option.id ? (
                                                    <RadioCheckedIcon />
                                                ) : (
                                                    <RadioUncheckedIcon />
                                                )}
                                            </button>
                                            <input
                                                type="text"
                                                className="option-input"
                                                placeholder={`Вариант ${String.fromCharCode(65 + optIndex)}`}
                                                value={option.text}
                                                onChange={(e) =>
                                                    handleOptionChange(question.id, option.id, e.target.value)
                                                }
                                            />
                                        </div>
                                    ))}
                                </div>
                            )}
                            {(question.questionType || "SINGLE_CHOICE") === "TEXT" && (
                                <div className="options-list">
                                    <textarea
                                        className="question-text-input"
                                        placeholder="Правильный текстовый ответ"
                                        value={question.correctTextAnswer || ""}
                                        onChange={(e) =>
                                            handleQuestionChange(question.id, "correctTextAnswer", e.target.value)
                                        }
                                        rows="2"
                                    />
                                </div>
                            )}
                            {(question.questionType || "SINGLE_CHOICE") === "OPEN" && (
                                <div className="field-helper">Открытый вопрос проверяется вручную, правильный ответ не задается.</div>
                            )}
                        </div>
                    ))}
                </div>

                <button className="btn-add-question" onClick={handleAddQuestion} type="button">
                    <PlusIcon />
                    Добавить вопрос
                </button>

                <div className="modal-actions">
                    <button className="btn-cancel" onClick={onClose} type="button" disabled={isSaving}>
                        Отмена
                    </button>
                    <button className="btn-save" onClick={handleSave} type="button" disabled={isSaving}>
                        {isSaving ? "Сохранение..." : "Сохранить вопросы"}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default TestModal;
