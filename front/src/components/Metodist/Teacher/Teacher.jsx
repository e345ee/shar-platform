import { useState } from "react";
import "./Teacher.css";
import {
    TeachersIcon,
    PlusIcon,
    EditIcon,
    TrashIcon,
    HomeIcon,
} from "../../../svgs/MethodistSvg.jsx";
import { BookIcon } from "../../../svgs/ClassSvg.jsx";
import { EmailIcon, PhoneIcon } from "../../../svgs/TeacherSvg.jsx";
import AddTeacherModal from "./AddTeacherModal";

function Teachers({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [teachers, setTeachers] = useState([
        {
            id: 1,
            name: "Иванова Мария Петровна",
            email: "maria.ivanova@example.com",
            phone: "+7 (999) 123-45-67",
            subject: "Математика",
            bio: "Преподаватель математики с 15-летним стажем. Кандидат педагогических наук.",
            photo: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=400&fit=crop",
            tg_id: "@maria_ivanova",
        },
        {
            id: 2,
            name: "Петров Алексей Иванович",
            email: "alexey.petrov@example.com",
            phone: "+7 (999) 234-56-78",
            subject: "Физика",
            bio: "Опытный преподаватель физики, автор более 20 научных статей.",
            photo: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=400&fit=crop",
            tg_id: "@alexey_petrov",
        },
        {
            id: 3,
            name: "Сидорова Елена Викторовна",
            email: "elena.sidorova@example.com",
            phone: "+7 (999) 345-67-89",
            subject: "Химия",
            bio: "Преподаватель химии высшей категории. Победитель конкурса \"Учитель года\".",
            photo: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400&h=400&fit=crop",
            tg_id: "@elena_sidorova",
        },
    ]);

    const totalTeachers = teachers.length;

    const handleAddTeacher = (newTeacherData) => {
        const newTeacher = {
            id: teachers.length + 1,
            ...newTeacherData,
        };
        setTeachers([...teachers, newTeacher]);
    };

    const handleDeleteTeacher = (id) => {
        setTeachers(teachers.filter((teacher) => teacher.id !== id));
    };

    return (
        <div className="teachers-management">
            <div className="teachers-container">
                <header className="teachers-header">
                    <div className="teachers-header-left">
                        <div className="teachers-header-icon">
                            <TeachersIcon />
                        </div>
                        <div>
                            <h1 className="teachers-title">Преподаватели</h1>
                            <p className="teachers-subtitle">Управление преподавательским составом</p>
                        </div>
                    </div>
                    <div className="teachers-header-actions">
                        <button className="btn-add-teacher" onClick={() => setShowModal(true)} type="button">
                            <PlusIcon />
                            Добавить преподавателя
                        </button>
                        <button className="btn-home" onClick={onBackToMain} type="button">
                            <HomeIcon />
                            На главную
                        </button>
                    </div>
                </header>

                <div className="teachers-stats">
                    <div className="stat-card stat-green">
                        <div className="stat-icon">
                            <TeachersIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Всего преподавателей</div>
                            <div className="stat-value">{totalTeachers}</div>
                        </div>
                    </div>
                </div>

                <section className="teachers-list-section">
                    <div className="teachers-list-header">
                        <h2 className="teachers-list-title">Список преподавателей</h2>
                        <p className="teachers-list-subtitle">Всего преподавателей: {totalTeachers}</p>
                    </div>
                    <div className="teachers-list">
                        {teachers.map((teacher) => (
                            <div key={teacher.id} className="teacher-card">
                                <div className="teacher-photo">
                                    <img src={teacher.photo} alt={teacher.name} />
                                </div>
                                <div className="teacher-info">
                                    <h3 className="teacher-name">{teacher.name}</h3>
                                    <div className="teacher-details">
                                        <div className="teacher-detail-item">
                                            <EmailIcon />
                                            <span>{teacher.email}</span>
                                        </div>
                                        <div className="teacher-detail-item">
                                            <PhoneIcon />
                                            <span>{teacher.phone}</span>
                                        </div>
                                        <div className="teacher-detail-item">
                                            <BookIcon />
                                            <span>{teacher.subject}</span>
                                        </div>
                                    </div>
                                    <p className="teacher-bio">{teacher.bio}</p>
                                </div>
                                <div className="teacher-actions">
                                    <button className="teacher-action-btn" type="button" aria-label="Edit">
                                        <EditIcon />
                                    </button>
                                    <button
                                        className="teacher-action-btn"
                                        type="button"
                                        onClick={() => handleDeleteTeacher(teacher.id)}
                                        aria-label="Delete"
                                    >
                                        <TrashIcon />
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>
            </div>

            <AddTeacherModal
                isOpen={showModal}
                onClose={() => setShowModal(false)}
                onAddTeacher={handleAddTeacher}
            />
        </div>
    );
}

export default Teachers;
