import { useEffect, useState } from "react";
import "./Class.css";
import { BookIcon, CloseIcon } from "../../../svgs/ClassSvg";

function AddCourseModal({
                            isOpen,
                            onClose,
                            onSubmitCourse,
                            isSubmitting,
                            errorMessage,
                            mode = "create",
                            initialData = null,
                        }) {
    const [formData, setFormData] = useState({
        name: "",
        description: "",
    });

    useEffect(() => {
        if (!isOpen) return;
        if (mode === "edit" && initialData) {
            setFormData({
                name: initialData.name || "",
                description: initialData.description || "",
            });
            return;
        }
        setFormData({ name: "", description: "" });
    }, [isOpen, mode, initialData]);

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.name.trim()) {
            return;
        }
        await onSubmitCourse({
            name: formData.name.trim(),
            description: formData.description.trim(),
        });
        setFormData({ name: "", description: "" });
        onClose();
    };

    const handleClose = () => {
        setFormData({ name: "", description: "" });
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={handleClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">{mode === "edit" ? "Редактировать курс" : "Создать курс"}</h2>
                <p className="modal-subtitle">
                    {mode === "edit"
                        ? "Изменения будут применены сразу после сохранения"
                        : "Курс можно будет выбрать при создании класса"}
                </p>
                {errorMessage && <div className="classes-error">{errorMessage}</div>}
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <BookIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Название курса</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: Математика"
                                value={formData.name}
                                onChange={(e) => handleInputChange("name", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <BookIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Описание</label>
                            <textarea
                                className="modal-textarea"
                                placeholder="Краткое описание курса"
                                value={formData.description}
                                onChange={(e) => handleInputChange("description", e.target.value)}
                                rows="4"
                            />
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn" disabled={isSubmitting}>
                        {mode === "edit" ? "Сохранить изменения" : "Создать курс"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddCourseModal;
