import { useState } from "react";
import "./StudyActivity.css";
import {
    ActivitiesIcon,
    PlusIcon,
    EditIcon,
    TrashIcon,
    HomeIcon,
} from "../../../svgs/MethodistSvg.jsx";
import {CalendarIcon, BookIcon, LocationIcon, DocumentIcon, TestIcon, QuestionsListIcon } from "../../../svgs/ActivitySvg.jsx";
import AddStudyActivityModal from "./AddStudyActivityModal";
import TextModal from "./TextModal";
import TestModal from "./TestModal";

function StudyActivity({ onBackToMain }) {
    const [showModal, setShowModal] = useState(false);
    const [showTextTasksModal, setShowTextTasksModal] = useState(false);
    const [showTestQuestionsModal, setShowTestQuestionsModal] = useState(false);
    const [selectedActivity, setSelectedActivity] = useState(null);
    const [activities, setActivities] = useState([
        {
            id: 1,
            title: "Контрольная работа по алгебре",
            topic: "Квадратные уравнения",
            format: "Контрольная работа",
            type: "Тест",
            class: "9А",
            date: "15.01.2024",
            description: "Решить 10 задач на тему квадратных уравнений. Время выполнения: 45 минут.",
            questionsCount: 2,
            color: "red",
            questions: [
                {
                    id: 1,
                    text: "Какое уравнение является квадратным?",
                    options: [
                        { id: 1, text: "x + 2 = 0" },
                        { id: 2, text: "x² + 3x + 2 = 0" },
                        { id: 3, text: "2x + 3 = 0" },
                    ],
                    correctAnswer: 2,
                },
                {
                    id: 2,
                    text: "Как найти дискриминант квадратного уравнения аx² + bx + c = 0?",
                    options: [
                        { id: 1, text: "b² - 4ac" },
                        { id: 2, text: "a² + b² + c²" },
                        { id: 3, text: "2ab + 3c" },
                    ],
                    correctAnswer: 1,
                },
            ],
        },
        {
            id: 2,
            title: "Домашнее задание: Литература",
            topic: "Творчество А.С. Пушкина",
            format: "Домашнее задание",
            type: "Текст",
            class: "10Б",
            date: "18.01.2024",
            description: "Прочитать главы 1-3 романа \"Евгений Онегин\". Выписать основные темы и мотивы.",
            questionsCount: 0,
            color: "blue",
            tasks: [],
        },
        {
            id: 3,
            title: "Еженедельный проект",
            topic: "Экология",
            format: "Еженедельное задание",
            type: "Текст",
            class: "11А",
            date: "20.01.2024",
            description: "Подготовить презентацию на тему \"Глобальное потепление\". Срок сдачи: до конца недели.",
            questionsCount: 0,
            color: "green",
            tasks: [],
        },
    ]);

    const stats = {
        tests: activities.filter(a => a.type === "Тест").length,
        homework: activities.filter(a => a.format === "Домашнее задание").length,
        weekly: activities.filter(a => a.format === "Еженедельное задание").length,
        lagging: activities.filter(a => a.format === "Для отстающих").length,
    };

    const totalActivities = activities.length;

    const handleAddActivity = (newActivityData) => {
        const colors = ["red", "blue", "green", "orange"];
        const randomColor = colors[Math.floor(Math.random() * colors.length)];

        const newActivity = {
            id: activities.length + 1,
            ...newActivityData,
            questionsCount: newActivityData.type === "Тест" ? 0 : 0,
            color: randomColor,
            tasks: newActivityData.type === "Текст" ? [] : undefined,
            questions: newActivityData.type === "Тест" ? [] : undefined,
        };
        setActivities([...activities, newActivity]);
    };

    const handleOpenTextTasks = (activity) => {
        setSelectedActivity(activity);
        setShowTextTasksModal(true);
    };

    const handleSaveTasks = (activityId, tasks) => {
        setActivities(activities.map(activity =>
            activity.id === activityId
                ? { ...activity, tasks, tasksCount: tasks.length }
                : activity
        ));
    };

    const handleOpenTestQuestions = (activity) => {
        setSelectedActivity(activity);
        setShowTestQuestionsModal(true);
    };

    const handleSaveQuestions = (activityId, questions) => {
        setActivities(activities.map(activity =>
            activity.id === activityId
                ? { ...activity, questions, questionsCount: questions.length }
                : activity
        ));
    };

    const handleDeleteActivity = (id) => {
        setActivities(activities.filter((activity) => activity.id !== id));
    };

    const getActivityTypeTag = (format, type) => {
        const formatColors = {
            "Контрольная работа": "red",
            "Домашнее задание": "blue",
            "Еженедельное задание": "green",
            "Для отстающих": "orange",
        };
        const typeColors = {
            "Тест": "pink",
            "Текст": "light-green",
        };
        return {
            format: { text: format, color: formatColors[format] || "gray" },
            type: { text: type, color: typeColors[type] || "gray" },
        };
    };

    return (
        <div className="activities-management">
            <div className="activities-container">
                <header className="activities-header">
                    <div className="activities-header-left">
                        <div className="activities-header-icon">
                            <ActivitiesIcon />
                        </div>
                        <div>
                            <h1 className="activities-title">Учебные активности</h1>
                            <p className="activities-subtitle">Создание и управление учебными заданиями</p>
                        </div>
                    </div>
                    <div className="activities-header-actions">
                        <button className="btn-add-activity" onClick={() => setShowModal(true)} type="button">
                            <PlusIcon />
                            Создать активность
                        </button>
                        <button className="btn-home" onClick={onBackToMain} type="button">
                            <HomeIcon />
                            На главную
                        </button>
                    </div>
                </header>

                <div className="activities-stats">
                    <div className="stat-card stat-red">
                        <div className="stat-icon">
                            <TestIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Тесты</div>
                            <div className="stat-value">{stats.tests}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-blue">
                        <div className="stat-icon">
                            <BookIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Домашние задания</div>
                            <div className="stat-value">{stats.homework}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-green">
                        <div className="stat-icon">
                            <CalendarIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Еженедельные</div>
                            <div className="stat-value">{stats.weekly}</div>
                        </div>
                    </div>
                    <div className="stat-card stat-orange">
                        <div className="stat-icon">
                            <BookIcon />
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">Для отстающих</div>
                            <div className="stat-value">{stats.lagging}</div>
                        </div>
                    </div>
                </div>

                <section className="activities-list-section">
                    <div className="activities-list-header">
                        <h2 className="activities-list-title">Список активностей</h2>
                        <p className="activities-list-subtitle">Всего активностей: {totalActivities}</p>
                    </div>
                    <div className="activities-list">
                        {activities.map((activity) => {
                            const tags = getActivityTypeTag(activity.format, activity.type);
                            return (
                                <div key={activity.id} className={`activity-card activity-card-${activity.color}`}>
                                    <div className={`activity-icon activity-icon-${activity.color}`}>
                                        <BookIcon />
                                    </div>
                                    <div className="activity-info">
                                        <div className="activity-header-info">
                                            <h3 className="activity-title">{activity.title}</h3>
                                            <div className="activity-tags">
                                                <span className={`activity-tag tag-${tags.format.color}`}>
                                                    {tags.format.text}
                                                </span>
                                                <span className={`activity-tag tag-${tags.type.color}`}>
                                                    {tags.type.text}
                                                </span>
                                            </div>
                                        </div>
                                        <div className="activity-details">
                                            <div className="activity-detail-item">
                                                <LocationIcon />
                                                <span>{activity.topic}</span>
                                            </div>
                                            <div className="activity-detail-item">
                                                <BookIcon />
                                                <span>Класс: {activity.class}</span>
                                            </div>
                                            <div className="activity-detail-item">
                                                <CalendarIcon />
                                                <span>{activity.date}</span>
                                            </div>
                                        </div>
                                        <p className="activity-description">{activity.description}</p>
                                        {activity.type === "Текст" && (
                                            <div className="activity-questions">
                                                <button
                                                    className="btn-questions"
                                                    type="button"
                                                    onClick={() => handleOpenTextTasks(activity)}
                                                >
                                                    <DocumentIcon />
                                                    {activity.tasks?.length || 0} заданий
                                                </button>
                                            </div>
                                        )}
                                        {activity.type === "Тест" && (
                                            <div className="activity-questions">
                                                <button
                                                    className="btn-questions"
                                                    type="button"
                                                    onClick={() => handleOpenTestQuestions(activity)}
                                                >
                                                    <QuestionsListIcon />
                                                    {activity.questionsCount || 0} вопросов
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                    <div className="activity-actions">
                                        <button className="activity-action-btn" type="button" aria-label="Edit">
                                            <EditIcon />
                                        </button>
                                        <button
                                            className="activity-action-btn"
                                            type="button"
                                            onClick={() => handleDeleteActivity(activity.id)}
                                            aria-label="Delete"
                                        >
                                            <TrashIcon />
                                        </button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>
            </div>

            <AddStudyActivityModal
                isOpen={showModal}
                onClose={() => setShowModal(false)}
                onAddActivity={handleAddActivity}
            />
            <TextModal
                isOpen={showTextTasksModal}
                onClose={() => {
                    setShowTextTasksModal(false);
                    setSelectedActivity(null);
                }}
                activity={selectedActivity}
                onSaveTasks={handleSaveTasks}
            />
            <TestModal
                isOpen={showTestQuestionsModal}
                onClose={() => {
                    setShowTestQuestionsModal(false);
                    setSelectedActivity(null);
                }}
                activity={selectedActivity}
                onSaveQuestions={handleSaveQuestions}
            />
        </div>
    );
}

export default StudyActivity;
