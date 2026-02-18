import { useEffect, useRef, useState } from "react";
import "./StudyMaterial.css";
import { MaterialsIcon, CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { FileIcon, UploadIcon, ChatIcon } from "../../../svgs/StudyMaterialSvg.jsx";

function AddStudyMaterialModal({
                                   isOpen,
                                   onClose,
                                   onSubmitMaterial,
                                   courses = [],
                                   mode = "create",
                                   initialData = null,
                                   isSubmitting = false,
                                   errorMessage = "",
                               }) {
    const [formData, setFormData] = useState({
        courseId: "",
        title: "",
        description: "",
        orderIndex: "",
        file: null,
    });
    const [dragActive, setDragActive] = useState(false);
    const [localError, setLocalError] = useState("");
    const fileInputRef = useRef(null);
    const isEditMode = mode === "edit";

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const setFileOrError = (file) => {
        if (!file) return;
        if (file.type !== "application/pdf") {
            setLocalError("Пожалуйста, загрузите файл в формате PDF");
            return;
        }
        if (file.size > 20 * 1024 * 1024) {
            setLocalError("Размер файла не должен превышать 20 MB");
            return;
        }
        setLocalError("");
        setFormData((prev) => ({
            ...prev,
            file,
        }));
    };

    const handleFileChange = (e) => {
        setFileOrError(e.target.files[0]);
    };

    const handleDrag = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (e.type === "dragenter" || e.type === "dragover") {
            setDragActive(true);
        } else if (e.type === "dragleave") {
            setDragActive(false);
        }
    };

    const handleDrop = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);
        setFileOrError(e.dataTransfer.files && e.dataTransfer.files[0]);
    };

    const formatFileSize = (bytes) => {
        if (bytes === 0) return "0 Bytes";
        const k = 1024;
        const sizes = ["Bytes", "KB", "MB", "GB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLocalError("");
        if (!isEditMode && !formData.courseId) {
            setLocalError("Выберите курс");
            return;
        }
        if (!formData.title.trim()) {
            setLocalError("Введите название урока");
            return;
        }
        if (!isEditMode && !formData.file) {
            setLocalError("Загрузите PDF-презентацию");
            return;
        }

        await onSubmitMaterial({
            id: initialData?.id,
            courseId: Number(formData.courseId || initialData?.courseId),
            title: formData.title.trim(),
            description: formData.description.trim(),
            orderIndex: formData.orderIndex ? Number(formData.orderIndex) : undefined,
            presentation: formData.file,
        });
        setFormData({
            courseId: "",
            title: "",
            description: "",
            orderIndex: "",
            file: null,
        });
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
        onClose();
    };

    const handleClose = () => {
        if (isSubmitting) return;
        setLocalError("");
        setFormData({
            courseId: "",
            title: "",
            description: "",
            orderIndex: "",
            file: null,
        });
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
        onClose();
    };

    useEffect(() => {
        if (!isOpen) return;
        setLocalError("");
        setFormData({
            courseId: initialData?.courseId ? String(initialData.courseId) : "",
            title: initialData?.title || "",
            description: initialData?.description || "",
            orderIndex: initialData?.orderIndex ? String(initialData.orderIndex) : "",
            file: null,
        });
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    }, [isOpen, initialData]);

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={handleClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">{isEditMode ? "Редактировать урок" : "Добавить урок"}</h2>
                <p className="modal-subtitle">
                    {isEditMode
                        ? "Измените данные урока. PDF можно заменить при необходимости."
                        : "Создайте урок и загрузите PDF-презентацию"}
                </p>
                {(errorMessage || localError) && <div className="materials-error">{errorMessage || localError}</div>}
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <MaterialsIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Курс <span className="required">*</span>
                            </label>
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
                            <MaterialsIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Название урока <span className="required">*</span>
                            </label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: Введение в программирование"
                                value={formData.title}
                                onChange={(e) => handleInputChange("title", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <ChatIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Описание</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Краткое описание содержания урока"
                                value={formData.description}
                                onChange={(e) => handleInputChange("description", e.target.value)}
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <ChatIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Порядок в курсе (опционально)</label>
                            <input
                                type="number"
                                min="1"
                                className="modal-input"
                                placeholder="Например: 3"
                                value={formData.orderIndex}
                                onChange={(e) => handleInputChange("orderIndex", e.target.value)}
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <FileIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                PDF файл {!isEditMode && <span className="required">*</span>}
                            </label>
                            <div
                                className={`file-upload-area ${dragActive ? "drag-active" : ""}`}
                                onDragEnter={handleDrag}
                                onDragLeave={handleDrag}
                                onDragOver={handleDrag}
                                onDrop={handleDrop}
                                onClick={() => fileInputRef.current?.click()}
                            >
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept=".pdf"
                                    onChange={handleFileChange}
                                    style={{ display: "none" }}
                                />
                                {formData.file ? (
                                    <div className="file-selected">
                                        <FileIcon />
                                        <div className="file-info">
                                            <div className="file-name">{formData.file.name}</div>
                                            <div className="file-size">{formatFileSize(formData.file.size)}</div>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="file-upload-placeholder">
                                        <UploadIcon />
                                        <p>{isEditMode ? "Нажмите, чтобы заменить PDF" : "Нажмите для загрузки PDF"}</p>
                                        <p className="file-size-limit">Максимальный размер: 20 MB</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn" disabled={isSubmitting}>
                        {isSubmitting ? "Сохранение..." : isEditMode ? "Сохранить изменения" : "Добавить урок"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddStudyMaterialModal;
