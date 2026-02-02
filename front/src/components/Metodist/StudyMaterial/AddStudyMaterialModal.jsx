import { useState, useRef } from "react";
import "./StudyMaterial.css";
import { MaterialsIcon, CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { FileIcon, UploadIcon, ChatIcon } from "../../../svgs/StudyMaterialSvg.jsx";

function AddStudyMaterialModal({ isOpen, onClose, onAddMaterial }) {
    const [formData, setFormData] = useState({
        title: "",
        description: "",
        file: null,
    });
    const [dragActive, setDragActive] = useState(false);
    const fileInputRef = useRef(null);

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.type !== "application/pdf") {
                alert("Пожалуйста, загрузите файл в формате PDF");
                return;
            }
            if (file.size > 50 * 1024 * 1024) {
                alert("Размер файла не должен превышать 50 MB");
                return;
            }
            setFormData((prev) => ({
                ...prev,
                file: file,
            }));
        }
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

        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            const file = e.dataTransfer.files[0];
            if (file.type !== "application/pdf") {
                alert("Пожалуйста, загрузите файл в формате PDF");
                return;
            }
            if (file.size > 50 * 1024 * 1024) {
                alert("Размер файла не должен превышать 50 MB");
                return;
            }
            setFormData((prev) => ({
                ...prev,
                file: file,
            }));
        }
    };

    const formatFileSize = (bytes) => {
        if (bytes === 0) return "0 Bytes";
        const k = 1024;
        const sizes = ["Bytes", "KB", "MB", "GB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + " " + sizes[i];
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (formData.title && formData.file) {
            const fileSize = formatFileSize(formData.file.size);
            const uploadDate = new Date().toLocaleDateString("ru-RU", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
            });

            onAddMaterial({
                title: formData.title,
                description: formData.description || "",
                fileName: formData.file.name,
                fileSize: fileSize,
                uploadDate: uploadDate,
            });
            setFormData({
                title: "",
                description: "",
                file: null,
            });
            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }
            onClose();
        }
    };

    const handleClose = () => {
        setFormData({
            title: "",
            description: "",
            file: null,
        });
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={handleClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">Добавить материал</h2>
                <p className="modal-subtitle">Загрузите новый учебный материал в формате PDF</p>
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <MaterialsIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                Название материала <span className="required">*</span>
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
                                placeholder="Краткое описание содержания материала"
                                value={formData.description}
                                onChange={(e) => handleInputChange("description", e.target.value)}
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <FileIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">
                                PDF файл <span className="required">*</span>
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
                                        <p>Нажмите для загрузки PDF</p>
                                        <p className="file-size-limit">Максимальный размер: 50 MB</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn">
                        Добавить материал
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddStudyMaterialModal;
