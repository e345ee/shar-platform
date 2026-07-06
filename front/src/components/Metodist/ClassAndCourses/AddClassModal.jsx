import { useEffect, useState } from "react";
import "./Class.css";
import { BuildingIcon, BookIcon, CloseIcon, TeacherIcon } from "../../../svgs/ClassSvg";

function AddClassModal({
                           isOpen,
                           onClose,
                           onSubmitClass,
                           courses,
                           teachers,
                           isSubmitting,
                           errorMessage,
                           mode = "create",
                           initialData = null,
                       }) {
    const [formData, setFormData] = useState({
        name: "",
        courseId: "",
        teacherId: "",
    });

    useEffect(() => {
        if (!isOpen) {
            return;
        }
        if (mode === "edit" && initialData) {
            setFormData({
                name: initialData.name || "",
                courseId: initialData.courseId ? String(initialData.courseId) : "",
                teacherId: initialData.teacherId ? String(initialData.teacherId) : "",
            });
            return;
        }
        setFormData({ name: "", courseId: "", teacherId: "" });
    }, [isOpen, mode, initialData]);

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (formData.name && formData.courseId) {
            await onSubmitClass({
                name: formData.name.trim(),
                courseId: Number(formData.courseId),
                teacherId: formData.teacherId ? Number(formData.teacherId) : null,
            });
            setFormData({ name: "", courseId: "", teacherId: "" });
            onClose();
        }
    };

    const handleClose = () => {
        setFormData({ name: "", courseId: "", teacherId: "" });
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={handleClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">{mode === "edit" ? "Редактировать класс" : "Добавить новый класс"}</h2>
                <p className="modal-subtitle">Заполните данные и выберите курс/преподавателя из БД</p>
                {errorMessage && <div className="classes-error">{errorMessage}</div>}
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <BuildingIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Номер класса</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: 10-А, 11-Б"
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
                            <label className="modal-label">Курс/Предмет</label>
                            <select
                                className="modal-input"
                                value={formData.courseId}
                                onChange={(e) => handleInputChange("courseId", e.target.value)}
                                required
                                disabled={mode === "edit"}
                            >
                                <option value="">Выберите курс</option>
                                {courses.map((course) => (
                                    <option key={course.id} value={course.id}>
                                        {course.name}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <TeacherIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Преподаватель</label>
                            <select
                                className="modal-input"
                                value={formData.teacherId}
                                onChange={(e) => handleInputChange("teacherId", e.target.value)}
                            >
                                <option value="">Без преподавателя</option>
                                {teachers.map((teacher) => (
                                    <option key={teacher.id} value={teacher.id}>
                                        {teacher.name}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn" disabled={isSubmitting}>
                        {mode === "edit" ? "Сохранить изменения" : "Добавить класс"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddClassModal;
