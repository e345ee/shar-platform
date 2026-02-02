import { useState } from "react";
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

function StudyMaterial({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [materials, setMaterials] = useState([
        {
            id: 1,
            title: "Введение в программирование",
            description: "Основные концепции программирования для начинающих",
            fileName: "intro-programming.pdf",
            fileSize: "2.4 MB",
            uploadDate: "15.01.2024",
            downloads: 145,
        },
        {
            id: 2,
            title: "Алгоритмы и структуры данных",
            description: "Подробное руководство по основным алгоритмам",
            fileName: "algorithms.pdf",
            fileSize: "5.1 MB",
            uploadDate: "20.01.2024",
            downloads: 198,
        },
        {
            id: 3,
            title: "Базы данных",
            description: "Введение в реляционные базы данных и SQL",
            fileName: "databases.pdf",
            fileSize: "3.8 MB",
            uploadDate: "25.01.2024",
            downloads: 89,
        },
    ]);

    const totalMaterials = materials.length;
    const totalDownloads = materials.reduce((sum, material) => sum + material.downloads, 0);

    const handleAddMaterial = (newMaterialData) => {
        const newMaterial = {
            id: materials.length + 1,
            ...newMaterialData,
            downloads: 0,
        };
        setMaterials([...materials, newMaterial]);
    };

    const handleDeleteMaterial = (id) => {
        setMaterials(materials.filter((material) => material.id !== id));
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
                        <button className="btn-add-material" onClick={() => setShowModal(true)} type="button">
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
                        <h2 className="materials-list-title">Список материалов</h2>
                        <p className="materials-list-subtitle">Все учебные материалы в системе</p>
                    </div>
                    <div className="materials-list">
                        {materials.map((material) => (
                            <div key={material.id} className="material-card">
                                <div className="material-icon">
                                    <FileIcon />
                                </div>
                                <div className="material-info">
                                    <h3 className="material-title">{material.title}</h3>
                                    <p className="material-description">{material.description}</p>
                                    <div className="material-file-info">
                                        <span className="material-file-name">{material.fileName}</span>
                                        <span className="material-file-size">{material.fileSize}</span>
                                        <span className="material-file-date">{material.uploadDate}</span>
                                    </div>
                                </div>
                                <div className="material-actions">
                                    <button className="material-action-btn" type="button" aria-label="Edit">
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
                onClose={() => setShowModal(false)}
                onAddMaterial={handleAddMaterial}
            />
        </div>
    );
}

export default StudyMaterial;
