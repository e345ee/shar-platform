import { useState } from "react";
import "./Class.css";
import {
    BuildingIcon,
    PeopleIcon,
    BookIcon,
    PlusIcon,
    EditIcon,
    TrashIcon,
    HomeIcon,
} from "../../../svgs/ClassSvg";
import { UserFieldIcon, PeopleFieldIcon } from "../../../svgs/MethodistSvg";
import AddClassModal from "./AddClassModal";

function Class({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [classes, setClasses] = useState([
        { id: 1, name: "10-A", course: "Математика", teacher: "Иванова Мария Петровна", students: 28 },
        { id: 2, name: "10-Б", course: "Физика", teacher: "Петров Алексей Иванович", students: 25 },
        { id: 3, name: "11-A", course: "Химия", teacher: "Сидорова Елена Викторовна", students: 22 },
        { id: 4, name: "9-A", course: "Биология", teacher: "Козлов Дмитрий Сергеевич", students: 30 },
    ]);

    const totalClasses = classes.length;
    const totalStudents = classes.reduce((sum, cls) => sum + cls.students, 0);
    const avgClassSize = Math.round(totalStudents / totalClasses);

    const handleAddClass = (newClassData) => {
        const newClass = {
            id: classes.length + 1,
            ...newClassData,
        };
        setClasses([...classes, newClass]);
    };

    const handleDeleteClass = (id) => {
        setClasses(classes.filter((cls) => cls.id !== id));
    };

    return (
        <div className="classes-management">
            <div className="classes-container">
                <header className="classes-header">
                    <div className="classes-header-left">
                        <div className="classes-header-icon">
                            <BuildingIcon />
                        </div>
                        <div>
                            <h1 className="classes-title">Управление классами</h1>
                            <p className="classes-subtitle">Список всех классов и возможность добавления новых</p>
                        </div>
                    </div>
                    <div className="classes-header-actions">
                        <button className="btn-add-class" onClick={() => setShowModal(true)} type="button">
                            <PlusIcon />
                            Добавить класс
                        </button>
                        <button className="btn-home" onClick={onBackToMain} type="button">
                            <HomeIcon />
                            На главную
                        </button>
                    </div>
                </header>

                <div className="classes-stats">
                    <div className="stat-card stat-blue">
                        <div className="stat-icon">
                            <BuildingIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Всего классов</div>
                            <div className="stat-value">{totalClasses}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-green">
                        <div className="stat-icon">
                            <PeopleIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Всего студентов</div>
                            <div className="stat-value">{totalStudents}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-magenta">
                        <div className="stat-icon">
                            <BookIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Средний размер класса</div>
                            <div className="stat-value">{avgClassSize}</div>
                        </div>
                    </div>
                </div>

                <section className="classes-list-section">
                    <div className="classes-list-header">
                        <h2 className="classes-list-title">Список классов</h2>
                        <p className="classes-list-subtitle">Все классы в системе</p>
                    </div>
                    <div className="classes-list">
                        {classes.map((cls) => (
                            <div key={cls.id} className="class-card">
                                <div className="class-icon">
                                    <span className="class-name-badge">{cls.name}</span>
                                </div>
                                <div className="class-info">
                                    <div className="class-field">
                                        <span className="class-field-label">Курс</span>
                                        <span className="class-field-value">{cls.course}</span>
                                    </div>
                                    <div className="class-field">
                                        <div className="class-field-with-icon">
                                            <UserFieldIcon />
                                            <span className="class-field-label">Преподаватель</span>
                                        </div>
                                        <span className="class-field-value">{cls.teacher}</span>
                                    </div>
                                    <div className="class-field">
                                        <div className="class-field-with-icon">
                                            <PeopleFieldIcon />
                                            <span className="class-field-label">Студентов</span>
                                        </div>
                                        <span className="class-field-value">{cls.students}</span>
                                    </div>
                                </div>
                                <div className="class-actions">
                                    <button className="class-action-btn" type="button" aria-label="Edit">
                                        <EditIcon />
                                    </button>
                                    <button
                                        className="class-action-btn"
                                        type="button"
                                        onClick={() => handleDeleteClass(cls.id)}
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

            <AddClassModal
                isOpen={showModal}
                onClose={() => setShowModal(false)}
                onAddClass={handleAddClass}
            />
        </div>
    );
}

export default Class;
