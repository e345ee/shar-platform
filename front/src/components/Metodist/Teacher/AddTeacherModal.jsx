import { useState } from "react";
import "./Teacher.css";
import { CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { EmailIcon, PhoneIcon, UserIcon, ImageIcon, TelegramIcon, LockIcon, ChatIcon } from "../../../svgs/TeacherSvg.jsx";

function AddTeacherModal({ isOpen, onClose, onAddTeacher }) {
    const [formData, setFormData] = useState({
        name: "",
        email: "",
        password: "",
        phone: "",
        bio: "",
        photo: "",
        tg_id: "",
    });

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (formData.name && formData.email && formData.password && formData.phone) {
            onAddTeacher({
                name: formData.name,
                email: formData.email,
                password: formData.password,
                phone: formData.phone,
                bio: formData.bio || "",
                photo: formData.photo || "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&h=400&fit=crop",
                tg_id: formData.tg_id || "",
            });
            setFormData({
                name: "",
                email: "",
                password: "",
                phone: "",
                bio: "",
                photo: "",
                tg_id: "",
            });
            onClose();
        }
    };

    const handleClose = () => {
        setFormData({
            name: "",
            email: "",
            password: "",
            phone: "",
            bio: "",
            photo: "",
            tg_id: "",
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
                <h2 className="modal-title">Добавить нового преподавателя</h2>
                <p className="modal-subtitle">Заполните информацию о новом преподавателе</p>
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <UserIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">ФИО преподавателя</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: Иванова Мария Петровна"
                                value={formData.name}
                                onChange={(e) => handleInputChange("name", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <EmailIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Email</label>
                            <input
                                type="email"
                                className="modal-input"
                                placeholder="example@email.com"
                                value={formData.email}
                                onChange={(e) => handleInputChange("email", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <LockIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Пароль</label>
                            <input
                                type="password"
                                className="modal-input"
                                placeholder="Введите пароль"
                                value={formData.password}
                                onChange={(e) => handleInputChange("password", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <PhoneIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Телефон</label>
                            <input
                                type="tel"
                                className="modal-input"
                                placeholder="+7 (999) 123-45-67"
                                value={formData.phone}
                                onChange={(e) => handleInputChange("phone", e.target.value)}
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
                                placeholder="https://example.com/photo.jpg"
                                value={formData.photo}
                                onChange={(e) => handleInputChange("photo", e.target.value)}
                            />
                            {formData.photo && (
                                <div className="image-preview">
                                    <img src={formData.photo} alt="Preview" onError={(e) => { e.target.style.display = 'none'; }} />
                                </div>
                            )}
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <TelegramIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Telegram ID</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="@username"
                                value={formData.tg_id}
                                onChange={(e) => handleInputChange("tg_id", e.target.value)}
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <ChatIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">О преподавателе (биография)</label>
                            <textarea
                                className="modal-textarea"
                                placeholder="Краткая информация о преподавателе, опыт работы, достижения"
                                value={formData.bio}
                                onChange={(e) => handleInputChange("bio", e.target.value)}
                                rows="4"
                            />
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn">
                        Добавить преподавателя
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddTeacherModal;
