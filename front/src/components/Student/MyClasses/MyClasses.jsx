import { useEffect, useState } from "react";
import { HomeIcon, BookIcon, ClassesIcon } from "../../../svgs/MethodistSvg";
import "./MyClasses.css";
import { createJoinRequest, listMyCourses } from "../../api/studentApi";

function MyClasses({ onBackToMain }) {
  const [classCode, setClassCode] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [courses, setCourses] = useState([]);

  useEffect(() => {
    let isCancelled = false;

    const load = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const coursesData = await listMyCourses();
        if (!isCancelled) {
          setCourses(coursesData);
        }
      } catch (e) {
        if (!isCancelled) {
          // Ignore error, just show empty list
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    load();
    return () => {
      isCancelled = true;
    };
  }, []);

  const handleSubmitRequest = async () => {
    const normalizedCode = String(classCode || "").trim().toUpperCase();
    if (!normalizedCode) {
      setErrorMessage("Введите код класса");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      await createJoinRequest(normalizedCode);
      setSuccessMessage("Заявка успешно отправлена! Ожидайте одобрения от преподавателя.");
      setClassCode("");
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось отправить заявку");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="my-classes-management">
      <div className="my-classes-container">
        <header className="my-classes-header">
          <div className="my-classes-header-left">
            <div>
              <h1 className="my-classes-title">Мои классы и заявки</h1>
              <p className="my-classes-subtitle">
                Подача заявки на вступление в класс и просмотр информации о ваших классах
              </p>
            </div>
          </div>
          <div className="my-classes-header-right">
            <button
              className="btn-home"
              onClick={onBackToMain}
              type="button"
            >
              <HomeIcon />
              На главную
            </button>
          </div>
        </header>

        {errorMessage && (
          <div className="my-classes-error">
            {errorMessage.includes("fetch") || errorMessage.includes("Failed")
              ? "Данные временно недоступны."
              : errorMessage}
          </div>
        )}

        {successMessage && (
          <div className="my-classes-success">{successMessage}</div>
        )}

        <div className="my-classes-card">
          <h3>Подать заявку на вступление в класс</h3>
          <p className="my-classes-muted">
            Введите код класса, который вам предоставил преподаватель, чтобы подать заявку на вступление
          </p>

          <div className="my-classes-inline-form">
            <input
              className="my-classes-input"
              type="text"
              placeholder="Код класса (например: ABC12345)"
              value={classCode}
              onChange={(e) => setClassCode(e.target.value.toUpperCase())}
              maxLength={8}
            />
            <button
              type="button"
              className="my-classes-btn my-classes-btn-primary"
              onClick={handleSubmitRequest}
              disabled={isSubmitting || !classCode.trim()}
            >
              {isSubmitting ? "Отправка..." : "Отправить заявку"}
            </button>
          </div>
        </div>

        <div className="my-classes-card">
          <h3>Мои курсы</h3>
          <p className="my-classes-muted">
            Курсы, в которых вы участвуете через классы
          </p>
          {isLoading ? (
            <div className="my-classes-empty">Загрузка...</div>
          ) : courses.length > 0 ? (
            <div className="my-classes-course-list">
              {courses.map((course) => (
                <div key={course.id} className="my-classes-course-item">
                  <div className="my-classes-course-info">
                    <h4>
                      <BookIcon />
                      {course.name || `Курс #${course.id}`}
                    </h4>
                    <p>{course.description || "Без описания"}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="my-classes-empty">
              Вы пока не участвуете ни в одном курсе. Подайте заявку на вступление в класс, чтобы начать обучение.
            </div>
          )}
        </div>

        <div className="my-classes-info-card">
          <h3>Информация о классах</h3>
          <ul>
            <li>
              Код класса предоставляется преподавателем или методистом
            </li>
            <li>
              После подачи заявки преподаватель рассмотрит её и примет решение
            </li>
            <li>
              После одобрения заявки вы будете добавлены в класс и получите доступ к учебным материалам
            </li>
            <li>
              Вы можете просмотреть ленту достижений класса в соответствующем разделе, указав ID класса
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default MyClasses;
