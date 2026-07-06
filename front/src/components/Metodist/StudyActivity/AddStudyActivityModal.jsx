import { useEffect, useState } from "react";
import "./StudyActivity.css"
import { ActivitiesIcon, CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { TagIcon, CalendarIcon, BookIcon, DocumentIcon, CheckIcon } from "../../../svgs/ActivitySvg.jsx";
import { listLessonsByCourse } from "../../api/methodistApi";

const ACTIVITY_TYPE_OPTIONS = [
    { value: "HOMEWORK_TEST", label: "Домашний тест" },
    { value: "CONTROL_WORK", label: "Контрольная работа" },
    { value: "WEEKLY_STAR", label: "Еженедельное задание" },
    { value: "REMEDIAL_TASK", label: "Для отстающих" },
];

function toDateTimeLocal(value) {
    if (!value) return "";
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        return String(value).slice(0, 16);
    }
    const yyyy = parsed.getFullYear();
    const mm = String(parsed.getMonth() + 1).padStart(2, "0");
    const dd = String(parsed.getDate()).padStart(2, "0");
    const hh = String(parsed.getHours()).padStart(2, "0");
    const mi = String(parsed.getMinutes()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
}

function AddStudyActivityModal({
                                   isOpen,
                                   onClose,
                                   onSubmitActivity,
                                   courses = [],
                                   errorMessage = "",
                                   isSubmitting = false,
                                   mode = "create",
                                   initialActivity = null,
                               }) {
    const [formData, setFormData] = useState({
        courseId: "",
        lessonId: "",
        activityType: "",
        title: "",
        topic: "",
        deadline: "",
        timeLimitSeconds: "",
        description: "",
    });
    const [lessons, setLessons] = useState([]);
    const [isLoadingLessons, setIsLoadingLessons] = useState(false);
    const [localError, setLocalError] = useState("");
    const isEditMode = mode === "edit";

    const selectedActivityType = formData.activityType;
    const needsLesson = selectedActivityType === "HOMEWORK_TEST" || selectedActivityType === "CONTROL_WORK";

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    useEffect(() => {
        if (!isOpen) {
            return;
        }
        setFormData({
            courseId: initialActivity?.courseId ? String(initialActivity.courseId) : "",
            lessonId: initialActivity?.lessonId ? String(initialActivity.lessonId) : "",
            activityType: initialActivity?.activityType || "",
            title: initialActivity?.title || "",
            topic: initialActivity?.topic || "",
            deadline: toDateTimeLocal(initialActivity?.deadline),
            timeLimitSeconds:
                initialActivity?.timeLimitSeconds !== null && initialActivity?.timeLimitSeconds !== undefined
                    ? String(initialActivity.timeLimitSeconds)
                    : "",
            description: initialActivity?.description || "",
        });
        setLessons([]);
        setLocalError("");
    }, [isOpen, initialActivity]);

    useEffect(() => {
        const courseId = Number(formData.courseId);
        if (!isOpen || !courseId) {
            setLessons([]);
            return;
        }

        let isCancelled = false;
        setIsLoadingLessons(true);
        setLocalError("");
        listLessonsByCourse(courseId)
            .then((data) => {
                if (isCancelled) return;
                setLessons(data);
            })
            .catch((error) => {
                if (isCancelled) return;
                setLessons([]);
                setLocalError(error?.message || "Не удалось загрузить уроки курса");
            })
            .finally(() => {
                if (isCancelled) return;
                setIsLoadingLessons(false);
            });

        return () => {
            isCancelled = true;
        };
    }, [formData.courseId, isOpen]);

    useEffect(() => {
        if (!needsLesson && formData.lessonId) {
            setFormData((prev) => ({ ...prev, lessonId: "" }));
        }
    }, [needsLesson, formData.lessonId]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLocalError("");

        const courseId = Number(formData.courseId);
        if (!courseId) {
            setLocalError("Выберите курс");
            return;
        }
        if (!formData.activityType) {
            setLocalError("Выберите формат активности");
            return;
        }
        if (needsLesson && !formData.lessonId) {
            setLocalError("Для выбранного формата нужно выбрать урок");
            return;
        }

        const payload = {
            title: formData.title.trim(),
            topic: formData.topic.trim(),
            description: formData.description.trim(),
            deadline: formData.deadline,
        };
        if (!isEditMode) {
            payload.activityType = formData.activityType;
            payload.lessonId = needsLesson && formData.lessonId ? Number(formData.lessonId) : null;
        }
        if (formData.activityType === "CONTROL_WORK" && formData.timeLimitSeconds) {
            payload.timeLimitSeconds = Number(formData.timeLimitSeconds);
        }

        await onSubmitActivity({
            id: initialActivity?.id,
            courseId,
            payload,
        });
        onClose();
    };

    const handleCourseChange = (courseId) => {
        setFormData((prev) => ({
            ...prev,
            courseId,
            lessonId: "",
        }));
    };

    const handleClose = () => {
        if (isSubmitting) {
            return;
        }
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
                <h2 className="modal-title">{isEditMode ? "Редактировать активность" : "Создать учебную активность"}</h2>
                <p className="modal-subtitle">
                    {isEditMode
                        ? "Измените данные активности и сохраните изменения"
                        : "Заполните информацию для создания новой учебной активности"}
                </p>
                {(errorMessage || localError) && <div className="activity-error">{errorMessage || localError}</div>}
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <BookIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Курс <span className="required">*</span>
                            </label>
                            <select
                                className="modal-select"
                                value={formData.courseId}
                                onChange={(e) => handleCourseChange(e.target.value)}
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
                                value={formData.activityType}
                                onChange={(e) => handleInputChange("activityType", e.target.value)}
                                disabled={isEditMode}
                                required
                            >
                                <option value="">Выберите тип активности</option>
                                {ACTIVITY_TYPE_OPTIONS.map((item) => (
                                    <option key={item.value} value={item.value}>
                                        {item.label}
                                    </option>
                                ))}
                            </select>
                            <p className="field-helper">Формат определяет логику проверки и привязку к уроку</p>
                        </div>
                    </div>

                    {needsLesson && (
                        <div className="modal-field">
                            <div className="modal-field-icon">
                                <TagIcon />
                            </div>
                            <div className="modal-field-content">
                                <label className="modal-label">
                                    Урок <span className="required">*</span>
                                </label>
                                <select
                                    className="modal-select"
                                    value={formData.lessonId}
                                    onChange={(e) => handleInputChange("lessonId", e.target.value)}
                                    required={needsLesson}
                                    disabled={isEditMode || !formData.courseId || isLoadingLessons}
                                >
                                    <option value="">
                                        {isLoadingLessons ? "Загрузка уроков..." : "Выберите урок"}
                                    </option>
                                    {lessons.map((lesson) => (
                                        <option key={lesson.id} value={lesson.id}>
                                            {lesson.orderIndex ? `${lesson.orderIndex}. ` : ""}{lesson.title}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>
                    )}

                    {formData.activityType === "CONTROL_WORK" && (
                        <div className="modal-field">
                            <div className="modal-field-icon">
                                <CheckIcon />
                            </div>
                            <div className="modal-field-content">
                                <label className="modal-label">Лимит времени (секунды)</label>
                                <input
                                    type="number"
                                    min="1"
                                    max="86400"
                                    className="modal-input"
                                    placeholder="Например: 3600"
                                    value={formData.timeLimitSeconds}
                                    onChange={(e) => handleInputChange("timeLimitSeconds", e.target.value)}
                                />
                            </div>
                        </div>
                    )}

                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <CheckIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Дедлайн <span className="required">*</span>
                            </label>
                            <input
                                type="datetime-local"
                                className="modal-input"
                                value={formData.deadline}
                                onChange={(e) => handleInputChange("deadline", e.target.value)}
                                required
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
                                value={formData.description}
                                onChange={(e) => handleInputChange("description", e.target.value)}
                                rows="6"
                                required
                            />
                            <p className="field-helper">Укажите детальное описание содержания активности</p>
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn" disabled={isSubmitting}>
                        {isSubmitting ? "Сохранение..." : isEditMode ? "Сохранить изменения" : "Создать активность"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddStudyActivityModal;
