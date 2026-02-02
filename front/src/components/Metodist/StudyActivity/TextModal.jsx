import { useState, useEffect } from "react";
import "./StudyActivity.css";
import { CloseIcon, PlusIcon, TrashIcon } from "../../../svgs/MethodistSvg.jsx";
import { DocumentIcon } from "../../../svgs/ActivitySvg.jsx";

function TextModal({ isOpen, onClose, activity, onSaveTasks }) {
    const [tasks, setTasks] = useState([]);

    useEffect(() => {
        if (activity) {
            setTasks(activity.tasks || []);
        }
    }, [activity]);

    const handleAddTask = () => {
        setTasks([...tasks, { id: Date.now(), text: "" }]);
    };

    const handleTaskChange = (id, text) => {
        setTasks(tasks.map(task => task.id === id ? { ...task, text } : task));
    };

    const handleDeleteTask = (id) => {
        setTasks(tasks.filter(task => task.id !== id));
    };

    const handleSave = () => {
        onSaveTasks(activity.id, tasks);
        onClose();
    };

    if (!isOpen || !activity) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content modal-content-large" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={onClose} type="button">
                    <CloseIcon />
                </button>
                <h2 className="modal-title">Текстовые задания: {activity.title}</h2>
                <p className="modal-subtitle">Добавьте текстовые задания для этой активности</p>

                <div className="tasks-list">
                    {tasks.map((task, index) => (
                        <div key={task.id} className="task-item">
                            <div className="task-number">{index + 1}</div>
                            <textarea
                                className="task-input"
                                placeholder="Введите текст задания..."
                                value={task.text}
                                onChange={(e) => handleTaskChange(task.id, e.target.value)}
                                rows="3"
                            />
                            <button
                                className="task-delete-btn"
                                onClick={() => handleDeleteTask(task.id)}
                                type="button"
                                aria-label="Delete"
                            >
                                <TrashIcon />
                            </button>
                        </div>
                    ))}
                </div>

                <button
                    className="btn-add-task"
                    onClick={handleAddTask}
                    type="button"
                >
                    <PlusIcon />
                    Добавить задание
                </button>

                <div className="modal-actions">
                    <button className="btn-cancel" onClick={onClose} type="button">
                        Отмена
                    </button>
                    <button className="btn-save" onClick={handleSave} type="button">
                        Сохранить задания
                    </button>
                </div>
            </div>
        </div>
    );
}

export default TextModal;
