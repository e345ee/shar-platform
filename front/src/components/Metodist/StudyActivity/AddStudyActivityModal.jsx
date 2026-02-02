import { useState } from "react";
import "./StudyActivity.css"
import { ActivitiesIcon, CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { TagIcon, CalendarIcon, BookIcon, DocumentIcon, CheckIcon } from "../../../svgs/ActivitySvg.jsx";

function AddStudyActivityModal({ isOpen, onClose, onAddActivity }) {
    const [formData, setFormData] = useState({
        title: "",
        topic: "",
        format: "",
        type: "",
        class: "",
        content: "",
    });

    const activityFormats = [
        "Контрольная работа",
        "Домашнее задание",
        "Еженедельное задание",
        "Для отстающих",
    ];

    const activityTypes = ["Тест", "Текст"];

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (formData.title && formData.topic && formData.format && formData.type && formData.content) {
            const uploadDate = new Date().toLocaleDateString("ru-RU", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
            });

            onAddActivity({
                title: formData.title,
                topic: formData.topic,
                format: formData.format,
                type: formData.type,
                class: formData.class || "",
                date: uploadDate,
                description: formData.content,
            });
            setFormData({
                title: "",
                topic: "",
                format: "",
                type: "",
                class: "",
                content: "",
            });
            onClose();
        }
    };

    const handleClose = () => {
        setFormData({
            title: "",
            topic: "",
            format: "",
            type: "",
            class: "",
            content: "",
        });
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={handleClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">Создать учебную активность</h2>
                <p className="modal-subtitle">Заполните информацию для создания новой учебной активности</p>
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <DocumentIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Название активности <span className="required">*</span>
                            </label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: Контрольная работа по алгебре"
                                value={formData.title}
                                onChange={(e) => handleInputChange("title", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <TagIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Тема <span className="required">*</span>
                            </label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: Квадратные уравнения"
                                value={formData.topic}
                                onChange={(e) => handleInputChange("topic", e.target.value)}
                                required
                            />
                            <p className="field-helper">Укажите тему учебной активности</p>
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <CheckIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Формат активности <span className="required">*</span>
                            </label>
                            <select
                                className="modal-select"
                                value={formData.format}
                                onChange={(e) => handleInputChange("format", e.target.value)}
                                required
                            >
                                <option value="">Выберите формат активности</option>
                                {activityFormats.map((format) => (
                                    <option key={format} value={format}>
                                        {format}
                                    </option>
                                ))}
                            </select>
                            <p className="field-helper">Выберите формат учебной активности</p>
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <CheckIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Вид задания <span className="required">*</span>
                            </label>
                            <select
                                className="modal-select"
                                value={formData.type}
                                onChange={(e) => handleInputChange("type", e.target.value)}
                                required
                            >
                                <option value="">Выберите вид задания</option>
                                {activityTypes.map((type) => (
                                    <option key={type} value={type}>
                                        {type}
                                    </option>
                                ))}
                            </select>
                            <p className="field-helper">Выберите вид задания</p>
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <BookIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Класс (опционально)</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: 9А, 10Б"
                                value={formData.class}
                                onChange={(e) => handleInputChange("class", e.target.value)}
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <DocumentIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Содержание <span className="required">*</span>
                            </label>
                            <textarea
                                className="modal-textarea"
                                placeholder="Опишите содержание активности, задания, требования..."
                                value={formData.content}
                                onChange={(e) => handleInputChange("content", e.target.value)}
                                rows="6"
                                required
                            />
                            <p className="field-helper">Укажите детальное описание содержания активности</p>
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn">
                        Создать активность
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddStudyActivityModal;
