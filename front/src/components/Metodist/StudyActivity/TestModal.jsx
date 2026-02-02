import { useState, useEffect } from "react";
import "./StudyActivity.css";
import { CloseIcon, PlusIcon, TrashIcon, RadioCheckedIcon, RadioUncheckedIcon } from "../../../svgs/MethodistSvg.jsx";

function TestModal({ isOpen, onClose, activity, onSaveQuestions }) {
    const [questions, setQuestions] = useState([]);

    useEffect(() => {
        if (activity) {
            setQuestions(activity.questions || []);
        }
    }, [activity]);

    const handleAddQuestion = () => {
        setQuestions([
            ...questions,
            {
                id: Date.now(),
                text: "",
                options: [
                    { id: 1, text: "" },
                    { id: 2, text: "" },
                    { id: 3, text: "" },
                ],
                correctAnswer: null,
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

    const handleAddOption = (questionId) => {
        setQuestions(
            questions.map((question) =>
                question.id === questionId
                    ? {
                        ...question,
                        options: [
                            ...question.options,
                            { id: Date.now(), text: "" },
                        ],
                    }
                    : question
            )
        );
    };

    const handleDeleteOption = (questionId, optionId) => {
        setQuestions(
            questions.map((question) =>
                question.id === questionId
                    ? {
                        ...question,
                        options: question.options.filter((option) => option.id !== optionId),
                        correctAnswer:
                            question.correctAnswer === optionId
                                ? null
                                : question.correctAnswer,
                    }
                    : question
            )
        );
    };

    const handleDeleteQuestion = (questionId) => {
        setQuestions(questions.filter((question) => question.id !== questionId));
    };

    const handleSave = () => {
        onSaveQuestions(activity.id, questions);
        onClose();
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
                                        {question.options.length > 2 && (
                                            <button
                                                className="option-delete-btn"
                                                onClick={() =>
                                                    handleDeleteOption(question.id, option.id)
                                                }
                                                type="button"
                                                aria-label="Delete option"
                                            >
                                                <CloseIcon />
                                            </button>
                                        )}
                                    </div>
                                ))}
                                <button
                                    className="btn-add-option"
                                    onClick={() => handleAddOption(question.id)}
                                    type="button"
                                >
                                    <PlusIcon />
                                    Добавить вариант
                                </button>
                            </div>
                        </div>
                    ))}
                </div>

                <button className="btn-add-question" onClick={handleAddQuestion} type="button">
                    <PlusIcon />
                    Добавить вопрос
                </button>

                <div className="modal-actions">
                    <button className="btn-cancel" onClick={onClose} type="button">
                        Отмена
                    </button>
                    <button className="btn-save" onClick={handleSave} type="button">
                        Сохранить вопросы
                    </button>
                </div>
            </div>
        </div>
    );
}

export default TestModal;
