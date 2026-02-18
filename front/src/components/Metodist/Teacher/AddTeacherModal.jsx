import { useState } from "react";
import "./Teacher.css";
import { CloseIcon } from "../../../svgs/MethodistSvg.jsx";
import { EmailIcon, UserIcon, TelegramIcon, LockIcon } from "../../../svgs/TeacherSvg.jsx";

function AddTeacherModal({ isOpen, onClose, onAddTeacher }) {
    const [formData, setFormData] = useState({
        name: "",
        email: "",
        password: "",
        tgId: "",
    });

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (formData.name && formData.email && formData.password) {
            try {
                await onAddTeacher({
                    name: formData.name.trim(),
                    email: formData.email.trim(),
                    password: formData.password,
                    tgId: formData.tgId.trim(),
                });
                setFormData({
                    name: "",
                    email: "",
                    password: "",
                    tgId: "",
                });
                onClose();
            } catch (e) {
                // Parent component renders API error message and keeps modal open.
            }
        }
    };

    const handleClose = () => {
        setFormData({
            name: "",
            email: "",
            password: "",
            tgId: "",
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
                <h2 className="modal-title">Добавить преподавателя</h2>
                <p className="modal-subtitle">Доступные поля: имя, email, пароль, telegram id</p>
                <form onSubmit={handleSubmit} className="modal-form">
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <UserIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Имя</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: Иванов Иван"
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
                                placeholder="Минимум 6 символов"
                                value={formData.password}
                                onChange={(e) => handleInputChange("password", e.target.value)}
                                required
                            />
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
                                value={formData.tgId}
                                onChange={(e) => handleInputChange("tgId", e.target.value)}
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
