import { useState } from "react";
import "./Achivments.css";
import {
    AchievementsIcon,
    PlusIcon,
    EditIcon,
    TrashIcon,
    HomeIcon,
} from "../../../svgs/MethodistSvg";
import AddAchievementModal from "./AddAchievementModal";

function Achievements({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [achievements, setAchievements] = useState([
        {
            id: 1,
            title: "Первый курс",
            description: "Создайте свой первый курс в системе",
            imageUrl: "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=400&h=400&fit=crop",
        },
        {
            id: 2,
            title: "Мастер обучения",
            description: "Обучите более 100 студентов",
            imageUrl: "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=400&h=400&fit=crop",
        },
        {
            id: 3,
            title: "Отличная оценка",
            description: "Получите средний балл 4.8+ по всем курсам",
            imageUrl: "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=400&h=400&fit=crop",
        },
    ]);

    const totalAchievements = achievements.length;

    const handleAddAchievement = (newAchievementData) => {
        const newAchievement = {
            id: achievements.length + 1,
            ...newAchievementData,
        };
        setAchievements([...achievements, newAchievement]);
    };

    const handleDeleteAchievement = (id) => {
        setAchievements(achievements.filter((achievement) => achievement.id !== id));
    };

    return (
        <div className="achievements-management">
            <div className="achievements-container">
                <header className="achievements-header">
                    <div className="achievements-header-left">
                        <div className="achievements-header-icon">
                            <AchievementsIcon />
                        </div>
                        <div>
                            <h1 className="achievements-title">Достижения</h1>
                            <p className="achievements-subtitle">Управление достижениями студентов</p>
                        </div>
                    </div>
                    <div className="achievements-header-actions">
                        <button className="btn-add-achievement" onClick={() => setShowModal(true)} type="button">
                            <PlusIcon />
                            Добавить достижение
                        </button>
                        <button className="btn-home" onClick={onBackToMain} type="button">
                            <HomeIcon />
                            На главную
                        </button>
                    </div>
                </header>

                <div className="achievements-stats">
                    <div className="stat-card stat-yellow">
                        <div className="stat-icon">
                            <AchievementsIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Всего достижений</div>
                            <div className="stat-value">{totalAchievements}</div>
                        </div>
                    </div>
                </div>

                <section className="achievements-list-section">
                    <div className="achievements-list-header">
                        <h2 className="achievements-list-title">Список достижений</h2>
                        <p className="achievements-list-subtitle">Все достижения в системе</p>
                    </div>
                    <div className="achievements-list">
                        {achievements.map((achievement) => (
                            <div key={achievement.id} className="achievement-card">
                                <div className="achievement-image">
                                    <img src={achievement.imageUrl} alt={achievement.title} />
                                </div>
                                <div className="achievement-info">
                                    <h3 className="achievement-title">{achievement.title}</h3>
                                    <p className="achievement-description">{achievement.description}</p>
                                </div>
                                <div className="achievement-actions">
                                    <button className="achievement-action-btn" type="button" aria-label="Edit">
                                        <EditIcon />
                                    </button>
                                    <button
                                        className="achievement-action-btn"
                                        type="button"
                                        onClick={() => handleDeleteAchievement(achievement.id)}
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

            <AddAchievementModal
                isOpen={showModal}
                onClose={() => setShowModal(false)}
                onAddAchievement={handleAddAchievement}
            />
        </div>
    );
}

export default Achievements;
