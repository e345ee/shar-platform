import { useEffect, useMemo, useState } from "react";
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
import AddCourseModal from "./AddCourseModal";
import {
    createClass,
    createCourse,
    deleteCourse,
    deleteClass,
    getClassStudentsCount,
    listCourses,
    listMyClasses,
    listTeachers,
    updateCourse,
    updateClass,
} from "../../api/methodistApi";

function Class({ onBackToMain }) {
    const [showClassModal, setShowClassModal] = useState(false);
    const [showCourseModal, setShowCourseModal] = useState(false);
    const [editingClass, setEditingClass] = useState(null);
    const [editingCourse, setEditingCourse] = useState(null);
    const [classes, setClasses] = useState([]);
    const [courses, setCourses] = useState([]);
    const [teachers, setTeachers] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [classErrorMessage, setClassErrorMessage] = useState("");
    const [courseErrorMessage, setCourseErrorMessage] = useState("");
    const [activeListView, setActiveListView] = useState("classes");

    const courseNameById = useMemo(
        () => new Map(courses.map((course) => [course.id, course.name])),
        [courses]
    );
    const classCountByCourseId = useMemo(() => {
        const map = new Map();
        for (const cls of classes) {
            if (!cls.courseId) continue;
            map.set(cls.courseId, (map.get(cls.courseId) || 0) + 1);
        }
        return map;
    }, [classes]);

    const totalClasses = classes.length;
    const totalStudents = classes.reduce((sum, cls) => sum + (cls.studentsCount || 0), 0);
    const avgClassSize = totalClasses > 0 ? Math.round(totalStudents / totalClasses) : 0;

    const loadAll = async () => {
        setIsLoading(true);
        setClassErrorMessage("");
        setCourseErrorMessage("");
        try {
            const [classesData, coursesData, teachersData] = await Promise.all([
                listMyClasses(),
                listCourses(),
                listTeachers(),
            ]);

            const classesWithCounts = await Promise.all(
                classesData.map(async (sc) => ({
                    ...sc,
                    studentsCount: await getClassStudentsCount(sc.id),
                }))
            );

            setClasses(classesWithCounts);
            setCourses(coursesData);
            setTeachers(teachersData);
        } catch (error) {
            const message = error?.message || "Не удалось загрузить данные";
            setClassErrorMessage(message);
            setCourseErrorMessage(message);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadAll();
    }, []);

    const handleCreateClass = async (newClassData) => {
        setIsSubmitting(true);
        setClassErrorMessage("");
        try {
            await createClass(newClassData);
            await loadAll();
        } catch (error) {
            setClassErrorMessage(error?.message || "Не удалось создать класс");
            throw error;
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleUpdateClass = async (classData) => {
        if (!editingClass?.id) return;
        setIsSubmitting(true);
        setClassErrorMessage("");
        try {
            await updateClass(editingClass.id, classData);
            await loadAll();
            setEditingClass(null);
        } catch (error) {
            setClassErrorMessage(error?.message || "Не удалось обновить класс");
            throw error;
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleCreateCourse = async (courseData) => {
        setIsSubmitting(true);
        setCourseErrorMessage("");
        try {
            await createCourse(courseData);
            await loadAll();
        } catch (error) {
            setCourseErrorMessage(error?.message || "Не удалось создать курс");
            throw error;
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleUpdateCourse = async (courseData) => {
        if (!editingCourse?.id) return;
        setIsSubmitting(true);
        setCourseErrorMessage("");
        try {
            await updateCourse(editingCourse.id, courseData);
            await loadAll();
            setEditingCourse(null);
        } catch (error) {
            setCourseErrorMessage(error?.message || "Не удалось обновить курс");
            throw error;
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDeleteCourse = async (id) => {
        setCourseErrorMessage("");
        try {
            await deleteCourse(id);
            setCourses((prev) => prev.filter((course) => course.id !== id));
        } catch (error) {
            setCourseErrorMessage(error?.message || "Не удалось удалить курс");
        }
    };

    const handleDeleteClass = async (id) => {
        setClassErrorMessage("");
        try {
            await deleteClass(id);
            setClasses((prev) => prev.filter((cls) => cls.id !== id));
        } catch (error) {
            setClassErrorMessage(error?.message || "Не удалось удалить класс");
        }
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
                            <h1 className="classes-title">Курсы и классы</h1>
                            <p className="classes-subtitle">Управление курсами и классами из базы данных</p>
                        </div>
                    </div>
                    <div className="classes-header-actions">
                        <button
                            className="btn-add-class btn-add-course"
                            onClick={() => {
                                setCourseErrorMessage("");
                                setShowCourseModal(true);
                            }}
                            type="button"
                        >
                            <BookIcon />
                            Создать курс
                        </button>
                        <button
                            className="btn-add-class"
                            onClick={() => {
                                setClassErrorMessage("");
                                setEditingClass(null);
                                setShowClassModal(true);
                            }}
                            type="button"
                        >
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

                <div className="list-switcher">
                    <button
                        className={`list-switch-btn ${activeListView === "classes" ? "active" : ""}`}
                        type="button"
                        onClick={() => setActiveListView("classes")}
                    >
                        Список классов
                    </button>
                    <button
                        className={`list-switch-btn ${activeListView === "courses" ? "active" : ""}`}
                        type="button"
                        onClick={() => setActiveListView("courses")}
                    >
                        Список курсов
                    </button>
                </div>

                {activeListView === "classes" && (
                    <section className="classes-list-section">
                        <div className="classes-list-header">
                            <h2 className="classes-list-title">Список классов</h2>
                            <p className="classes-list-subtitle">Мои классы</p>
                        </div>
                        {classErrorMessage && (
                            <div className="classes-error">
                                <span>{classErrorMessage}</span>
                                <button
                                    className="classes-error-close"
                                    type="button"
                                    onClick={() => setClassErrorMessage("")}
                                    aria-label="Закрыть уведомление"
                                >
                                    ×
                                </button>
                            </div>
                        )}
                        <div className="classes-list">
                            {isLoading && <div className="classes-empty">Загрузка...</div>}
                            {!isLoading && classes.length === 0 && (
                                <div className="classes-empty">Классов пока нет</div>
                            )}
                            {classes.map((cls) => (
                                <div key={cls.id} className="class-card">
                                    <div className="class-icon">
                                        <span className="class-name-badge">{cls.name}</span>
                                    </div>
                                    <div className="class-info">
                                        <div className="class-field">
                                            <span className="class-field-label">Курс</span>
                                            <span className="class-field-value">
                                            {courseNameById.get(cls.courseId) || `ID курса: ${cls.courseId}`}
                                        </span>
                                        </div>
                                        <div className="class-field">
                                            <div className="class-field-with-icon">
                                                <UserFieldIcon />
                                                <span className="class-field-label">Преподаватель</span>
                                            </div>
                                            <span className="class-field-value">{cls.teacherName || "Не назначен"}</span>
                                        </div>
                                        <div className="class-field">
                                            <div className="class-field-with-icon">
                                                <PeopleFieldIcon />
                                                <span className="class-field-label">Студентов</span>
                                            </div>
                                            <span className="class-field-value">{cls.studentsCount || 0}</span>
                                        </div>
                                    </div>
                                    <div className="class-actions">
                                        <button
                                            className="class-action-btn"
                                            type="button"
                                            onClick={() => {
                                                setEditingClass(cls);
                                                setShowClassModal(true);
                                            }}
                                            aria-label="Edit"
                                        >
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
                )}

                {activeListView === "courses" && (
                    <section className="courses-list-section">
                        <div className="classes-list-header">
                            <h2 className="classes-list-title">Список курсов</h2>
                            <p className="classes-list-subtitle">Курсы, доступные для создания классов</p>
                        </div>
                        {courseErrorMessage && (
                            <div className="classes-error">
                                <span>{courseErrorMessage}</span>
                                <button
                                    className="classes-error-close"
                                    type="button"
                                    onClick={() => setCourseErrorMessage("")}
                                    aria-label="Закрыть уведомление"
                                >
                                    ×
                                </button>
                            </div>
                        )}
                        {courses.length === 0 ? (
                            <div className="classes-empty">Курсов пока нет</div>
                        ) : (
                            <div className="courses-list">
                                {courses.map((course) => (
                                    <div key={course.id} className="class-card course-card">
                                        <div className="course-info">
                                            <div className="class-field">
                                                <span className="class-field-label">Название курса</span>
                                                <span className="class-field-value">{course.name}</span>
                                            </div>
                                            <div className="class-field">
                                                <span className="class-field-label">Описание</span>
                                                <span className="class-field-value">{course.description || "нет информации"}</span>
                                            </div>
                                            <div className="class-field">
                                                <span className="class-field-label">Классов</span>
                                                <span className="class-field-value">{classCountByCourseId.get(course.id) || 0}</span>
                                            </div>
                                            <div className="class-field">
                                                <span className="class-field-label">Создал</span>
                                                <span className="class-field-value">{course.createdByName || "-"}</span>
                                            </div>
                                        </div>
                                        <div className="class-actions">
                                            <button
                                                className="class-action-btn"
                                                type="button"
                                                onClick={() => {
                                                    setCourseErrorMessage("");
                                                    setEditingCourse(course);
                                                    setShowCourseModal(true);
                                                }}
                                                aria-label="Edit course"
                                            >
                                                <EditIcon />
                                            </button>
                                            <button
                                                className="class-action-btn"
                                                type="button"
                                                onClick={() => handleDeleteCourse(course.id)}
                                                aria-label="Delete course"
                                            >
                                                <TrashIcon />
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </section>
                )}
            </div>

            <AddClassModal
                isOpen={showClassModal}
                onClose={() => {
                    setShowClassModal(false);
                    setEditingClass(null);
                }}
                onSubmitClass={editingClass ? handleUpdateClass : handleCreateClass}
                courses={courses}
                teachers={teachers}
                isSubmitting={isSubmitting}
                errorMessage={classErrorMessage}
                mode={editingClass ? "edit" : "create"}
                initialData={editingClass}
            />
            <AddCourseModal
                isOpen={showCourseModal}
                onClose={() => {
                    setShowCourseModal(false);
                    setEditingCourse(null);
                }}
                onSubmitCourse={editingCourse ? handleUpdateCourse : handleCreateCourse}
                isSubmitting={isSubmitting}
                errorMessage={courseErrorMessage}
                mode={editingCourse ? "edit" : "create"}
                initialData={editingCourse}
            />
        </div>
    );
}

export default Class;
