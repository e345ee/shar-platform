import { useEffect, useMemo, useState } from "react";
import "./Statistics.css";
import {
    DownloadIcon,
    HomeIcon,
    StatisticsIcon,
    TeachersIcon,
} from "../../../svgs/MethodistSvg.jsx";
import {
    downloadTeacherStatisticsCsv,
    listTeacherStatistics,
} from "../../api/methodistApi";

function formatPercent(value) {
    if (value == null) {
        return "—";
    }
    const num = Number(value);
    if (Number.isNaN(num)) {
        return "—";
    }
    return `${num.toFixed(1)}%`;
}

function Statistics({ onBackToMain }) {
    const [rows, setRows] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isDownloading, setIsDownloading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    const loadStatistics = async () => {
        setIsLoading(true);
        setErrorMessage("");
        try {
            const data = await listTeacherStatistics();
            setRows(data);
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось загрузить статистику");
            setRows([]);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadStatistics();
    }, []);

    const totals = useMemo(() => {
        return rows.reduce(
            (acc, row) => {
                acc.teachers += 1;
                acc.classes += Number(row.classesCount || 0);
                acc.students += Number(row.studentsCount || 0);
                return acc;
            },
            { teachers: 0, classes: 0, students: 0 },
        );
    }, [rows]);

    const handleDownloadCsv = async () => {
        setIsDownloading(true);
        setErrorMessage("");
        try {
            const blob = await downloadTeacherStatisticsCsv();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = "teachers_statistics.csv";
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось скачать CSV");
        } finally {
            setIsDownloading(false);
        }
    };

    return (
        <div className="methodist-statistics">
            <div className="methodist-statistics-container">
                <header className="methodist-statistics-header">
                    <div className="methodist-statistics-header-left">
                        <div className="methodist-statistics-header-icon">
                            <StatisticsIcon />
                        </div>
                        <div>
                            <h1 className="methodist-statistics-title">Статистика преподавателей</h1>
                            <p className="methodist-statistics-subtitle">
                                Аналитика по классам, студентам и проверкам
                            </p>
                        </div>
                    </div>
                    <div className="methodist-statistics-actions">
                        <button
                            className="btn-statistics-download"
                            onClick={handleDownloadCsv}
                            type="button"
                            disabled={isDownloading}
                        >
                            <DownloadIcon />
                            {isDownloading ? "Скачивание..." : "Скачать CSV"}
                        </button>
                        <button className="btn-statistics-home" onClick={onBackToMain} type="button">
                            <HomeIcon />
                            На главную
                        </button>
                    </div>
                </header>

                <section className="methodist-statistics-summary">
                    <div className="methodist-statistics-card">
                        <div className="methodist-statistics-card-icon">
                            <TeachersIcon />
                        </div>
                        <div className="methodist-statistics-card-label">Преподавателей</div>
                        <div className="methodist-statistics-card-value">{totals.teachers}</div>
                    </div>
                    <div className="methodist-statistics-card">
                        <div className="methodist-statistics-card-label">Классов</div>
                        <div className="methodist-statistics-card-value">{totals.classes}</div>
                    </div>
                    <div className="methodist-statistics-card">
                        <div className="methodist-statistics-card-label">Студентов</div>
                        <div className="methodist-statistics-card-value">{totals.students}</div>
                    </div>
                </section>

                <section className="methodist-statistics-table-section">
                    {errorMessage && <div className="methodist-statistics-error">{errorMessage}</div>}

                    {isLoading ? (
                        <div className="methodist-statistics-empty">Загрузка...</div>
                    ) : rows.length === 0 ? (
                        <div className="methodist-statistics-empty">Данные пока отсутствуют</div>
                    ) : (
                        <div className="methodist-statistics-table-wrap">
                            <table className="methodist-statistics-table">
                                <thead>
                                <tr>
                                    <th>Преподаватель</th>
                                    <th>Email</th>
                                    <th>Классы</th>
                                    <th>Студенты</th>
                                    <th>Отправлено работ</th>
                                    <th>Проверено работ</th>
                                    <th>Средний балл</th>
                                </tr>
                                </thead>
                                <tbody>
                                {rows.map((row) => (
                                    <tr key={row.teacherId}>
                                        <td>{row.teacherName || "—"}</td>
                                        <td>{row.teacherEmail || "—"}</td>
                                        <td>{row.classesCount}</td>
                                        <td>{row.studentsCount}</td>
                                        <td>{row.submittedAttemptsCount}</td>
                                        <td>{row.gradedAttemptsCount}</td>
                                        <td>{formatPercent(row.avgGradePercent)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
}

export default Statistics;
