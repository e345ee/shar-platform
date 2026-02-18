import { useEffect, useRef, useState } from "react";
import "./Achivments.css";
import { AchievementsIcon, CloseIcon } from "../../../svgs/MethodistSvg";
import { BookIcon } from "../../../svgs/ClassSvg";
import { ImageIcon } from "../../../svgs/AchivmentSvg";

function AddAchievementModal({
                                 isOpen,
                                 onClose,
                                 onSubmitAchievement,
                                 courses = [],
                                 mode = "create",
                                 initialData = null,
                                 isSubmitting = false,
                                 errorMessage = "",
                             }) {
    const [formData, setFormData] = useState({
        courseId: "",
        title: "",
        jokeDescription: "",
        description: "",
        photo: null,
    });
    const [localError, setLocalError] = useState("");
    const fileInputRef = useRef(null);
    const isEditMode = mode === "edit";

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    useEffect(() => {
        if (!isOpen) return;
        setLocalError("");
        setFormData({
            courseId: initialData?.courseId ? String(initialData.courseId) : "",
            title: initialData?.title || "",
            jokeDescription: initialData?.jokeDescription || "",
            description: initialData?.description || "",
            photo: null,
        });
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    }, [isOpen, initialData]);

    const setFileOrError = (file) => {
        if (!file) return;
        if (!file.type.startsWith("image/")) {
            setLocalError("Пожалуйста, загрузите изображение");
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            setLocalError("Размер изображения не должен превышать 5 MB");
            return;
        }
        setLocalError("");
        setFormData((prev) => ({
            ...prev,
            photo: file,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLocalError("");
        if (!isEditMode && !formData.courseId) {
            setLocalError("Выберите курс");
            return;
        }
        if (!formData.title.trim()) {
            setLocalError("Введите название достижения");
            return;
        }
        if (!formData.jokeDescription.trim()) {
            setLocalError("Введите короткое описание");
            return;
        }
        if (!formData.description.trim()) {
            setLocalError("Введите полное описание");
            return;
        }
        if (!isEditMode && !formData.photo) {
            setLocalError("Загрузите фотографию достижения");
            return;
        }

        await onSubmitAchievement({
            id: initialData?.id,
            courseId: Number(formData.courseId || initialData?.courseId),
            title: formData.title.trim(),
            jokeDescription: formData.jokeDescription.trim(),
            description: formData.description.trim(),
            photo: formData.photo,
        });
        onClose();
    };

    const handleClose = () => {
        if (isSubmitting) return;
        setLocalError("");
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={handleClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">{isEditMode ? "Редактировать достижение" : "Добавить достижение"}</h2>
                <p className="modal-subtitle">
                    {isEditMode
                        ? "Измените текст и при необходимости замените фото"
                        : "Создайте достижение и привяжите его к курсу"}
                </p>
                {(errorMessage || localError) && <div className="achievements-error">{errorMessage || localError}</div>}
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <BookIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Курс</label>
                            <select
                                className="modal-select"
                                value={formData.courseId}
                                onChange={(e) => handleInputChange("courseId", e.target.value)}
                                disabled={isEditMode}
                                required
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
                            <AchievementsIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Название достижения</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: Первый курс, Мастер обучения"
                                value={formData.title}
                                onChange={(e) => handleInputChange("title", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <BookIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Короткое описание (jokeDescription)</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Краткая подпись к достижению"
                                value={formData.jokeDescription}
                                onChange={(e) => handleInputChange("jokeDescription", e.target.value)}
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
                                placeholder="Опишите условие получения достижения"
                                value={formData.description}
                                onChange={(e) => handleInputChange("description", e.target.value)}
                                rows="4"
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <ImageIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Фотография</label>
                            <input
                                ref={fileInputRef}
                                type="file"
                                className="modal-input"
                                accept="image/*"
                                onChange={(e) => setFileOrError(e.target.files?.[0])}
                            />
                            {(formData.photo || initialData?.photoUrl) && (
                                <div className="image-preview">
                                    <img
                                        src={formData.photo ? URL.createObjectURL(formData.photo) : initialData?.photoUrl}
                                        alt="Preview"
                                    />
                                </div>
                            )}
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn" disabled={isSubmitting}>
                        {isSubmitting ? "Сохранение..." : isEditMode ? "Сохранить изменения" : "Добавить достижение"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddAchievementModal;
