import { useEffect, useMemo, useState } from "react";
import "./AchievementAssignment.css";
import { HomeIcon } from "../../../svgs/TeacherSvg.jsx";
import { AchievementsIcon } from "../../../svgs/MethodistSvg.jsx";
import {
    awardAchievementToStudent,
    listAchievementsByCourse,
    listClassStudents,
    listMyClasses,
    listStudentAchievements,
    revokeAchievementFromStudent,
} from "../../api/teacherApi";

function AchievementAssignment({ onBackToMain }) {
    const [classes, setClasses] = useState([]);
    const [students, setStudents] = useState([]);
    const [achievements, setAchievements] = useState([]);
    const [studentAchievements, setStudentAchievements] = useState([]);
    const [selectedClassId, setSelectedClassId] = useState(null);
    const [selectedStudentId, setSelectedStudentId] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isSavingId, setIsSavingId] = useState(null);
    const [errorMessage, setErrorMessage] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    useEffect(() => {
        let isCancelled = false;
        const loadClasses = async () => {
            setIsLoading(true);
            setErrorMessage("");
            try {
                const classesData = await listMyClasses();
                if (!isCancelled) {
                    setClasses(classesData);
                    setSelectedClassId(classesData[0]?.id ?? null);
                }
            } catch (e) {
                if (!isCancelled) {
                    setErrorMessage(e?.message || "Не удалось загрузить классы");
                }
            } finally {
                if (!isCancelled) {
                    setIsLoading(false);
                }
            }
        };
        loadClasses();
        return () => {
            isCancelled = true;
        };
    }, []);

    useEffect(() => {
        if (!selectedClassId) {
            setStudents([]);
            setAchievements([]);
            setSelectedStudentId(null);
            return;
        }

        let isCancelled = false;
        const loadClassData = async () => {
            setIsLoading(true);
            setErrorMessage("");
            try {
                const classItem = classes.find((c) => c.id === selectedClassId);
                const courseId = classItem?.courseId;
                const [studentsData, achievementsData] = await Promise.all([
                    listClassStudents(selectedClassId, 0, 100),
                    courseId ? listAchievementsByCourse(courseId) : Promise.resolve([]),
                ]);
                if (!isCancelled) {
                    const loadedStudents = studentsData?.content || [];
                    setStudents(loadedStudents);
                    setAchievements(achievementsData || []);
                    setSelectedStudentId((prev) =>
                        loadedStudents.some((s) => s.id === prev) ? prev : loadedStudents[0]?.id ?? null,
                    );
                }
            } catch (e) {
                if (!isCancelled) {
                    setErrorMessage(e?.message || "Не удалось загрузить учеников и достижения");
                    setStudents([]);
                    setAchievements([]);
                }
            } finally {
                if (!isCancelled) {
                    setIsLoading(false);
                }
            }
        };
        loadClassData();
        return () => {
            isCancelled = true;
        };
    }, [selectedClassId, classes]);

    useEffect(() => {
        if (!selectedStudentId) {
            setStudentAchievements([]);
            return;
        }

        let isCancelled = false;
        const loadStudentAchievements = async () => {
            setErrorMessage("");
            try {
                const data = await listStudentAchievements(selectedStudentId);
                if (!isCancelled) {
                    setStudentAchievements(data || []);
                }
            } catch (e) {
                if (!isCancelled) {
                    setErrorMessage(e?.message || "Не удалось загрузить достижения ученика");
                    setStudentAchievements([]);
                }
            }
        };
        loadStudentAchievements();
        return () => {
            isCancelled = true;
        };
    }, [selectedStudentId]);

    const assignedAchievementIds = useMemo(() => {
        return new Set(studentAchievements.map((a) => a.achievementId).filter(Boolean));
    }, [studentAchievements]);

    const selectedStudent = students.find((s) => s.id === selectedStudentId);
    const selectedClass = classes.find((c) => c.id === selectedClassId);
    const assignedCount = assignedAchievementIds.size;
    const totalCount = achievements.length;
    const availableCount = Math.max(0, totalCount - assignedCount);

    const formatDate = (value) => {
        if (!value) return "—";
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) return String(value);
        return parsed.toLocaleString("ru-RU");
    };

    const handleAward = async (achievementId) => {
        if (!selectedStudentId || !achievementId) return;
        setErrorMessage("");
        setSuccessMessage("");
        setIsSavingId(achievementId);
        try {
            await awardAchievementToStudent(achievementId, selectedStudentId);
            const refreshed = await listStudentAchievements(selectedStudentId);
            setStudentAchievements(refreshed || []);
            setSuccessMessage("Достижение успешно назначено");
        } catch (e) {
            setErrorMessage(e?.message || "Не удалось назначить достижение");
        } finally {
            setIsSavingId(null);
        }
    };

    const handleRevoke = async (achievementId) => {
        if (!selectedStudentId || !achievementId) return;
        setErrorMessage("");
        setSuccessMessage("");
        setIsSavingId(achievementId);
        try {
            await revokeAchievementFromStudent(achievementId, selectedStudentId);
            const refreshed = await listStudentAchievements(selectedStudentId);
            setStudentAchievements(refreshed || []);
            setSuccessMessage("Достижение снято");
        } catch (e) {
            setErrorMessage(e?.message || "Не удалось снять достижение");
        } finally {
            setIsSavingId(null);
        }
    };

    const handleRefresh = async () => {
        if (!selectedClassId) return;
        setErrorMessage("");
        setSuccessMessage("");
        setIsLoading(true);
        try {
            const classItem = classes.find((c) => c.id === selectedClassId);
            const courseId = classItem?.courseId;
            const [studentsData, achievementsData, awardedData] = await Promise.all([
                listClassStudents(selectedClassId, 0, 100),
                courseId ? listAchievementsByCourse(courseId) : Promise.resolve([]),
                selectedStudentId ? listStudentAchievements(selectedStudentId) : Promise.resolve([]),
            ]);
            const loadedStudents = studentsData?.content || [];
            setStudents(loadedStudents);
            setAchievements(achievementsData || []);
            setStudentAchievements(awardedData || []);
        } catch (e) {
            setErrorMessage(e?.message || "Не удалось обновить данные");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="achievement-assignment-management">
            <div className="achievement-assignment-container">
                <header className="achievement-assignment-header">
                    <div className="achievement-assignment-header-left">
                        <div className="achievement-assignment-header-icon">
                            <AchievementsIcon />
                        </div>
                        <div>
                            <h1 className="achievement-assignment-title">Назначение достижений</h1>
                            <p className="achievement-assignment-subtitle">
                                Выберите класс, ученика и назначьте достижения из курса
                            </p>
                        </div>
                    </div>
                    <div className="achievement-assignment-header-actions">
                        <button
                            className="btn-secondary"
                            onClick={handleRefresh}
                            type="button"
                            disabled={isLoading || !selectedClassId}
                        >
                            Обновить
                        </button>
                        <button className="btn-home" onClick={onBackToMain} type="button">
                            <HomeIcon />
                            На главную
                        </button>
                    </div>
                </header>

                {errorMessage ? <div className="status-message error">{errorMessage}</div> : null}
                {successMessage ? <div className="status-message success">{successMessage}</div> : null}

                <div className="achievement-assignment-stats">
                    <div className="stat-card stat-orange">
                        <div className="stat-content">
                            <div className="stat-label">Всего достижений курса</div>
                            <div className="stat-value">{totalCount}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-orange-light">
                        <div className="stat-content">
                            <div className="stat-label">Уже назначено</div>
                            <div className="stat-value">{assignedCount}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-orange-muted">
                        <div className="stat-content">
                            <div className="stat-label">Можно назначить</div>
                            <div className="stat-value">{availableCount}</div>
                        </div>
                    </div>
                </div>

                <div className="assignment-columns">
                    <section className="assignment-column">
                        <h3>Классы</h3>
                        <div className="items-list">
                            {classes.map((classItem) => (
                                <button
                                    key={classItem.id}
                                    type="button"
                                    className={`item-button ${selectedClassId === classItem.id ? "active" : ""}`}
                                    onClick={() => {
                                        setSelectedClassId(classItem.id);
                                        setSuccessMessage("");
                                    }}
                                >
                                    <strong>{classItem.name || `Класс #${classItem.id}`}</strong>
                                    <span>{classItem.courseName || "Без названия курса"}</span>
                                </button>
                            ))}
                            {!isLoading && classes.length === 0 ? (
                                <div className="empty-box">Нет доступных классов</div>
                            ) : null}
                        </div>
                    </section>

                    <section className="assignment-column">
                        <h3>Ученики</h3>
                        <div className="items-list">
                            {students.map((student) => (
                                <button
                                    key={student.id}
                                    type="button"
                                    className={`item-button ${selectedStudentId === student.id ? "active" : ""}`}
                                    onClick={() => {
                                        setSelectedStudentId(student.id);
                                        setSuccessMessage("");
                                    }}
                                >
                                    <strong>{student.name || `Ученик #${student.id}`}</strong>
                                    <span>{student.email || "Без email"}</span>
                                </button>
                            ))}
                            {!isLoading && selectedClassId && students.length === 0 ? (
                                <div className="empty-box">В выбранном классе нет учеников</div>
                            ) : null}
                        </div>
                    </section>
                </div>

                <section className="achievements-block">
                    <div className="achievements-block-header">
                        <h3>Список достижений</h3>
                        <p>
                            {selectedStudent
                                ? `Ученик: ${selectedStudent.name || "—"}`
                                : "Выберите ученика для назначения достижений"}
                            {selectedClass ? ` • Класс: ${selectedClass.name || "—"}` : ""}
                        </p>
                    </div>

                    {isLoading ? <div className="empty-box">Загрузка...</div> : null}

                    {!isLoading && achievements.length === 0 ? (
                        <div className="empty-box">Для курса класса нет достижений</div>
                    ) : null}

                    {!isLoading && achievements.length > 0 ? (
                        <div className="achievement-grid">
                            {achievements.map((achievement) => {
                                const isAssigned = assignedAchievementIds.has(achievement.id);
                                const isPending = isSavingId === achievement.id;
                                return (
                                    <article
                                        key={achievement.id}
                                        className={`achievement-card ${isAssigned ? "achievement-card-assigned" : ""}`}
                                    >
                                        {achievement.photoUrl ? (
                                            <img src={achievement.photoUrl} alt={achievement.title || "achievement"} />
                                        ) : (
                                            <div className="achievement-placeholder">
                                                <AchievementsIcon />
                                            </div>
                                        )}
                                        <div className="achievement-content">
                                            <div className="achievement-title-row">
                                                <strong>{achievement.title || "Без названия"}</strong>
                                                <span className={`state-badge ${isAssigned ? "assigned" : "available"}`}>
                          {isAssigned ? "Назначено" : "Доступно"}
                        </span>
                                            </div>
                                            <p>
                                                {achievement.jokeDescription ||
                                                    achievement.description ||
                                                    "Описание достижения отсутствует"}
                                            </p>
                                            <div className="achievement-meta">
                                                <span>Создано: {formatDate(achievement.createdAt)}</span>
                                            </div>
                                            <div className="achievement-actions">
                                                {!isAssigned ? (
                                                    <button
                                                        type="button"
                                                        className="btn-action btn-assign"
                                                        disabled={!selectedStudentId || isPending}
                                                        onClick={() => handleAward(achievement.id)}
                                                    >
                                                        {isPending ? "Назначение..." : "Назначить"}
                                                    </button>
                                                ) : (
                                                    <button
                                                        type="button"
                                                        className="btn-action btn-revoke"
                                                        disabled={!selectedStudentId || isPending}
                                                        onClick={() => handleRevoke(achievement.id)}
                                                    >
                                                        {isPending ? "Снятие..." : "Снять"}
                                                    </button>
                                                )}
                                            </div>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    ) : null}
                </section>
            </div>
        </div>
    );
}

export default AchievementAssignment;
