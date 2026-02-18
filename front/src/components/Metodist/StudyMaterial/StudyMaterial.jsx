import { useEffect, useMemo, useState } from "react";
import "./StudyMaterial.css";
import {
    MaterialsIcon,
    PlusIcon,
    EditIcon,
    TrashIcon,
    HomeIcon,
} from "../../../svgs/MethodistSvg.jsx";
import { DownloadIcon, FileIcon } from "../../../svgs/StudyMaterialSvg";
import AddStudyMaterialModal from "./AddStudyMaterialModal";
import {
    createLesson,
    deleteLesson,
    listLessonsByCourse,
    listMyCourses,
    updateLesson,
} from "../../api/methodistApi";

function StudyMaterial({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [materials, setMaterials] = useState([]);
    const [courses, setCourses] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [modalMode, setModalMode] = useState("create");
    const [editingMaterial, setEditingMaterial] = useState(null);

    const courseNameById = useMemo(
        () => new Map(courses.map((course) => [course.id, course.name])),
        [courses]
    );

    const loadAll = async () => {
        setIsLoading(true);
        setErrorMessage("");
        try {
            const coursesData = await listMyCourses();
            const lessonsByCourse = await Promise.all(
                coursesData.map(async (course) => ({
                    course,
                    lessons: await listLessonsByCourse(course.id),
                }))
            );
            const mapped = lessonsByCourse.flatMap(({ course, lessons }) =>
                lessons.map((lesson) => {
                    const fileName = lesson.presentationUrl ? lesson.presentationUrl.split("/").pop() : "-";
                    const uploadDate = lesson.createdAt
                        ? new Date(lesson.createdAt).toLocaleDateString("ru-RU", {
                            day: "2-digit",
                            month: "2-digit",
                            year: "numeric",
                        })
                        : "-";
                    return {
                        id: lesson.id,
                        courseId: course.id,
                        courseName: course.name,
                        title: lesson.title,
                        description: lesson.description || "",
                        fileName: fileName || "-",
                        fileSize: "-",
                        uploadDate,
                        downloads: 0,
                        orderIndex: lesson.orderIndex,
                    };
                })
            );

            setCourses(coursesData);
            setMaterials(mapped);
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось загрузить уроки");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadAll();
    }, []);

    const totalMaterials = materials.length;
    const totalDownloads = materials.reduce((sum, material) => sum + material.downloads, 0);

    const handleSubmitMaterial = async (lessonData) => {
        setIsSubmitting(true);
        setErrorMessage("");
        try {
            if (modalMode === "edit" && lessonData.id) {
                const updated = await updateLesson(lessonData.id, lessonData);
                const fileName = updated.presentationUrl ? updated.presentationUrl.split("/").pop() : "-";
                const uploadDate = updated.createdAt
                    ? new Date(updated.createdAt).toLocaleDateString("ru-RU", {
                        day: "2-digit",
                        month: "2-digit",
                        year: "numeric",
                    })
                    : "-";
                setMaterials((prev) =>
                    prev.map((material) =>
                        material.id === lessonData.id
                            ? {
                                ...material,
                                title: updated.title,
                                description: updated.description || "",
                                orderIndex: updated.orderIndex,
                                fileName: fileName || material.fileName,
                                uploadDate,
                            }
                            : material
                    )
                );
            } else {
                const created = await createLesson(lessonData.courseId, lessonData);
                const fileName = created.presentationUrl ? created.presentationUrl.split("/").pop() : "-";
                const uploadDate = created.createdAt
                    ? new Date(created.createdAt).toLocaleDateString("ru-RU", {
                        day: "2-digit",
                        month: "2-digit",
                        year: "numeric",
                    })
                    : new Date().toLocaleDateString("ru-RU", {
                        day: "2-digit",
                        month: "2-digit",
                        year: "numeric",
                    });
                const newMaterial = {
                    id: created.id,
                    courseId: created.courseId,
                    courseName: courseNameById.get(created.courseId) || "-",
                    title: created.title,
                    description: created.description || "",
                    fileName: fileName || "-",
                    fileSize: "-",
                    uploadDate,
                    downloads: 0,
                    orderIndex: created.orderIndex,
                };
                setMaterials((prev) => [newMaterial, ...prev]);
            }
        } catch (error) {
            setErrorMessage(
                error?.message || (modalMode === "edit" ? "Не удалось обновить урок" : "Не удалось создать урок")
            );
            throw error;
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDeleteMaterial = async (id) => {
        setErrorMessage("");
        try {
            await deleteLesson(id);
            setMaterials((prev) => prev.filter((material) => material.id !== id));
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось удалить урок");
        }
    };

    const handleOpenCreateModal = () => {
        setErrorMessage("");
        setModalMode("create");
        setEditingMaterial(null);
        setShowModal(true);
    };

    const handleOpenEditModal = (material) => {
        setErrorMessage("");
        setModalMode("edit");
        setEditingMaterial(material);
        setShowModal(true);
    };

    return (
        <div className="materials-management">
            <div className="materials-container">
                <header className="materials-header">
                    <div className="materials-header-left">
                        <div className="materials-header-icon">
                            <MaterialsIcon />
                        </div>
                        <div>
                            <h1 className="materials-title">Учебные материалы</h1>
                            <p className="materials-subtitle">Управление учебными материалами и документами</p>
                        </div>
                    </div>
                    <div className="materials-header-actions">
                        <button className="btn-add-material" onClick={handleOpenCreateModal} type="button">
                            <PlusIcon />
                            Добавить материал
                        </button>
                        <button className="btn-home" onClick={onBackToMain} type="button">
                            <HomeIcon />
                            На главную
                        </button>
                    </div>
                </header>

                <div className="materials-stats">
                    <div className="stat-card stat-green">
                        <div className="stat-icon">
                            <FileIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Всего материалов</div>
                            <div className="stat-value">{totalMaterials}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-blue">
                        <div className="stat-icon">
                            <DownloadIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Загрузок</div>
                            <div className="stat-value">{totalDownloads}</div>
                        </div>
                    </div>
                </div>

                <section className="materials-list-section">
                    <div className="materials-list-header">
                        <h2 className="materials-list-title">Список уроков</h2>
                        <p className="materials-list-subtitle">Уроки с загруженными презентациями</p>
                    </div>
                    {errorMessage && <div className="materials-error">{errorMessage}</div>}
                    <div className="materials-list">
                        {isLoading && <div className="materials-empty">Загрузка...</div>}
                        {!isLoading && materials.length === 0 && (
                            <div className="materials-empty">Уроков пока нет</div>
                        )}
                        {materials.map((material) => (
                            <div key={material.id} className="material-card">
                                <div className="material-icon">
                                    <FileIcon />
                                </div>
                                <div className="material-info">
                                    <h3 className="material-title">{material.title}</h3>
                                    <p className="material-description">{material.description}</p>
                                    <div className="material-file-info">
                                        <span className="material-file-name">Курс: {material.courseName || "-"}</span>
                                        <span className="material-file-size">Урок #{material.orderIndex || "-"}</span>
                                        <span className="material-file-name">{material.fileName}</span>
                                        <span className="material-file-size">{material.fileSize}</span>
                                        <span className="material-file-date">{material.uploadDate}</span>
                                    </div>
                                </div>
                                <div className="material-actions">
                                    <button
                                        className="material-action-btn"
                                        type="button"
                                        aria-label="Edit"
                                        onClick={() => handleOpenEditModal(material)}
                                    >
                                        <EditIcon />
                                    </button>
                                    <button
                                        className="material-action-btn"
                                        type="button"
                                        onClick={() => handleDeleteMaterial(material.id)}
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

            <AddStudyMaterialModal
                isOpen={showModal}
                onClose={() => {
                    setShowModal(false);
                    setEditingMaterial(null);
                    setModalMode("create");
                }}
                onSubmitMaterial={handleSubmitMaterial}
                mode={modalMode}
                initialData={editingMaterial}
                courses={courses}
                isSubmitting={isSubmitting}
                errorMessage={errorMessage}
            />
        </div>
    );
}

export default StudyMaterial;
