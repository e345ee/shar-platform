import { useState } from "react";
import "./Achivments.css";
import { AchievementsIcon, CloseIcon } from "../../../svgs/MethodistSvg";
import { BookIcon } from "../../../svgs/ClassSvg";
import { ImageIcon } from "../../../svgs/AchivmentSvg";

function AddAchievementModal({ isOpen, onClose, onAddAchievement }) {
    const [formData, setFormData] = useState({
        title: "",
        description: "",
        imageUrl: "",
    });

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (formData.title && formData.description && formData.imageUrl) {
            onAddAchievement({
                title: formData.title,
                description: formData.description,
                imageUrl: formData.imageUrl,
            });
            setFormData({ title: "", description: "", imageUrl: "" });
            onClose();
        }
    };

    const handleClose = () => {
        setFormData({ title: "", description: "", imageUrl: "" });
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={handleClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">Добавить новое достижение</h2>
                <p className="modal-subtitle">Заполните информацию о новом достижении</p>
                <form onSubmit={handleSubmit} className="modal-form">
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
                            <label className="modal-label">URL фотографии</label>
                            <input
                                type="url"
                                className="modal-input"
                                placeholder="https://example.com/image.jpg"
                                value={formData.imageUrl}
                                onChange={(e) => handleInputChange("imageUrl", e.target.value)}
                                required
                            />
                            {formData.imageUrl && (
                                <div className="image-preview">
                                    <img src={formData.imageUrl} alt="Preview" onError={(e) => { e.target.style.display = 'none'; }} />
                                </div>
                            )}
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn">
                        Добавить достижение
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddAchievementModal;
