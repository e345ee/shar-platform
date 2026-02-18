import { useEffect, useState } from "react";
import "./Teacher.css";
import {
    TeachersIcon,
    PlusIcon,
    TrashIcon,
    HomeIcon,
} from "../../../svgs/MethodistSvg.jsx";
import { EmailIcon, TelegramIcon, UserIcon } from "../../../svgs/TeacherSvg.jsx";
import AddTeacherModal from "./AddTeacherModal";
import { createTeacher, deleteTeacher, listTeachers } from "../../api/methodistApi";

function Teachers({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [teachers, setTeachers] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    const totalTeachers = teachers.length;

    const loadTeachers = async () => {
        setIsLoading(true);
        setErrorMessage("");
        try {
            const data = await listTeachers();
            setTeachers(data);
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось загрузить преподавателей");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadTeachers();
    }, []);

    const handleAddTeacher = async (newTeacherData) => {
        setErrorMessage("");
        try {
            const created = await createTeacher(newTeacherData);
            setTeachers((prev) => [created, ...prev]);
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось создать преподавателя");
            throw error;
        }
    };

    const handleDeleteTeacher = async (id) => {
        setErrorMessage("");
        try {
            await deleteTeacher(id);
            setTeachers((prev) => prev.filter((teacher) => teacher.id !== id));
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось удалить преподавателя");
        }
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
                            <p className="teachers-subtitle">Преподователи,которые под вашим надзором!</p>
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
                        <p className="teachers-list-subtitle">
                            Всего преподавателей: {totalTeachers}.
                        </p>
                    </div>
                    {errorMessage && <div className="teachers-error">{errorMessage}</div>}
                    <div className="teachers-list">
                        {!isLoading && teachers.length === 0 && (
                            <div className="teachers-empty">Преподавателей пока нет</div>
                        )}
                        {isLoading && <div className="teachers-empty">Загрузка...</div>}
                        {teachers.map((teacher) => (
                            <div key={teacher.id} className="teacher-card">
                                <div className="teacher-photo">
                                    {teacher.photo ? (
                                        <img
                                            src={teacher.photo}
                                            alt={teacher.name}
                                        />
                                    ) : (
                                        <div className="teacher-photo-empty" aria-label="Фото не загружено">
                                            <UserIcon />
                                        </div>
                                    )}
                                </div>
                                <div className="teacher-info">
                                    <h3 className="teacher-name">{teacher.name}</h3>
                                    <div className="teacher-details">
                                        <div className="teacher-detail-item">
                                            <EmailIcon />
                                            <span>{teacher.email}</span>
                                        </div>
                                        <div className="teacher-detail-item">
                                            <TelegramIcon />
                                            <span>{teacher.tgId || "Не указан"}</span>
                                        </div>
                                    </div>
                                    <p className="teacher-bio">{teacher.bio || "нет информации"}</p>
                                </div>
                                <div className="teacher-actions">
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
