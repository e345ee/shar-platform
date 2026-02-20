import { useEffect, useState } from "react";
import "./Achivments.css";
import {
    AchievementsIcon,
    PlusIcon,
    EditIcon,
    TrashIcon,
    HomeIcon,
} from "../../../svgs/MethodistSvg";
import AddAchievementModal from "./AddAchievementModal";
import {
    createAchievement,
    deleteAchievement,
    listAchievementsByCourse,
    listMyCourses,
    updateAchievement,
} from "../../api/methodistApi";

function Achievements({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [achievements, setAchievements] = useState([]);
    const [courses, setCourses] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [modalMode, setModalMode] = useState("create");
    const [editingAchievement, setEditingAchievement] = useState(null);

    const loadAll = async () => {
        setIsLoading(true);
        setErrorMessage("");
        try {
            const coursesData = await listMyCourses();
            const byCourse = await Promise.all(
                coursesData.map(async (course) => ({
                    course,
                    achievements: await listAchievementsByCourse(course.id),
                }))
            );
            const flat = byCourse.flatMap(({ course, achievements: items }) =>
                items.map((item) => ({
                    ...item,
                    courseName: course.name,
                }))
            );
            setCourses(coursesData);
            setAchievements(flat);
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось загрузить достижения");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadAll();
    }, []);

    const totalAchievements = achievements.length;

    const handleSubmitAchievement = async (payload) => {
        setIsSubmitting(true);
        setErrorMessage("");
        try {
            if (modalMode === "edit" && payload.id) {
                const updated = await updateAchievement(payload.id, payload);
                const currentCourseName = achievements.find((item) => item.id === payload.id)?.courseName || "-";
                setAchievements((prev) =>
                    prev.map((item) =>
                        item.id === payload.id
                            ? {
                                ...item,
                                ...updated,
                                courseName: currentCourseName,
                            }
                            : item
                    )
                );
            } else {
                const created = await createAchievement(payload.courseId, payload);
                const courseName = courses.find((course) => course.id === payload.courseId)?.name || "-";
                setAchievements((prev) => [{ ...created, courseName }, ...prev]);
            }
        } catch (error) {
            setErrorMessage(
                error?.message || (modalMode === "edit" ? "Не удалось обновить достижение" : "Не удалось создать достижение")
            );
            throw error;
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDeleteAchievement = async (id) => {
        setErrorMessage("");
        try {
            await deleteAchievement(id);
            setAchievements((prev) => prev.filter((achievement) => achievement.id !== id));
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось удалить достижение");
        }
    };

    const handleOpenCreateModal = () => {
        setErrorMessage("");
        setModalMode("create");
        setEditingAchievement(null);
        setShowModal(true);
    };

    const handleOpenEditModal = (achievement) => {
        setErrorMessage("");
        setModalMode("edit");
        setEditingAchievement(achievement);
        setShowModal(true);
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
                        <button className="btn-add-achievement" onClick={handleOpenCreateModal} type="button">
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
                    </div>
                    {errorMessage && <div className="achievements-error">{errorMessage}</div>}
                    <div className="achievements-list methodist-achievement-list">
                        {isLoading && <div className="achievements-empty">Загрузка...</div>}
                        {!isLoading && achievements.length === 0 && (
                            <div className="achievements-empty">Достижений пока нет</div>
                        )}
                        {achievements.map((achievement) => (
                            <div key={achievement.id} className="methodist-achievement-row">
                                <div className="methodist-achievement-image">
                                    <img src={achievement.photoUrl} alt={achievement.title} />
                                </div>
                                <div className="methodist-achievement-info">
                                    <h3 className="methodist-achievement-title">{achievement.title}</h3>
                                    <p className="methodist-achievement-course-badge">Курс: {achievement.courseName || "-"}</p>
                                    {achievement.jokeDescription ? (
                                        <p className="methodist-achievement-description">{achievement.jokeDescription}</p>
                                    ) : null}
                                    {achievement.description ? (
                                        <p className="methodist-achievement-description methodist-achievement-description-muted">
                                            {achievement.description}
                                        </p>
                                    ) : null}
                                </div>
                                <div className="methodist-achievement-actions">
                                    <button
                                        className="methodist-achievement-action-btn"
                                        type="button"
                                        aria-label="Edit"
                                        onClick={() => handleOpenEditModal(achievement)}
                                    >
                                        <EditIcon />
                                    </button>
                                    <button
                                        className="methodist-achievement-action-btn"
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
                onClose={() => {
                    setShowModal(false);
                    setEditingAchievement(null);
                    setModalMode("create");
                }}
                onSubmitAchievement={handleSubmitAchievement}
                courses={courses}
                mode={modalMode}
                initialData={editingAchievement}
                isSubmitting={isSubmitting}
                errorMessage={errorMessage}
            />
        </div>
    );
}

export default Achievements;
