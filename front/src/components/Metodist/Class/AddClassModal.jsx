import { useState } from "react";
import "./Class.css";
import { BuildingIcon, BookIcon, CloseIcon, TeacherIcon } from "../../../svgs/ClassSvg";

function AddClassModal({ isOpen, onClose, onAddClass }) {
    const [formData, setFormData] = useState({
        name: "",
        course: "",
        teacher: "",
    });

    const handleInputChange = (field, value) => {
        setFormData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (formData.name && formData.course && formData.teacher) {
            onAddClass({
                name: formData.name,
                course: formData.course,
                teacher: formData.teacher,
                students: 0,
            });
            setFormData({ name: "", course: "", teacher: "" });
            onClose();
        }
    };

    const handleClose = () => {
        setFormData({ name: "", course: "", teacher: "" });
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={handleClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">Добавить новый класс</h2>
                <p className="modal-subtitle">Заполните информацию о новом классе</p>
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
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="Например: Математика, Физика"
                                value={formData.course}
                                onChange={(e) => handleInputChange("course", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div className="modal-field">
                        <div className="modal-field-icon">
                            <TeacherIcon />
                        </div>
                        <div className="modal-field-content">
                            <label className="modal-label">Преподаватель</label>
                            <input
                                type="text"
                                className="modal-input"
                                placeholder="ФИО преподавателя"
                                value={formData.teacher}
                                onChange={(e) => handleInputChange("teacher", e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <button type="submit" className="modal-submit-btn">
                        Добавить класс
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AddClassModal;
