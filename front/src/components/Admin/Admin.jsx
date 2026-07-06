import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import "./Admin.css";
import imgLogo from "../../images/image.svg";
import {
  createMethodist,
  deleteMethodist,
  listMethodists,
} from "../api/adminApi"

function Admin({ onLogout }) {
  const [methodists, setMethodists] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isListLoading, setIsListLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [methodistForm, setMethodistForm] = useState({
    name: "",
    email: "",
    password: "",
    tgId: "",
  });

  const refreshMethodists = async () => {
    setIsListLoading(true);
    try {
      const rows = await listMethodists();
      setMethodists(rows);
    } catch (e) {
      setMethodists([]);
    } finally {
      setIsListLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setIsListLoading(true);
      try {
        const rows = await listMethodists();
        if (!cancelled) {
          setMethodists(rows);
        }
      } catch (e) {
        if (!cancelled) {
          setMethodists([]);
        }
      } finally {
        if (!cancelled) {
          setIsListLoading(false);
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const navigateTo = (path) => {
    window.history.pushState({}, "", path);
    window.dispatchEvent(new PopStateEvent("popstate"));
  };

  const handleAddMethodist = async (event) => {
    event.preventDefault();
    setErrorMessage("");
    setMessage("");
    if (
        !methodistForm.name.trim() ||
        !methodistForm.email.trim() ||
        !methodistForm.password.trim()
    ) {
      setErrorMessage("Заполните обязательные поля");
      return;
    }
    setIsLoading(true);
    try {
      await createMethodist({
        name: methodistForm.name.trim(),
        email: methodistForm.email.trim(),
        password: methodistForm.password.trim(),
        tgId: methodistForm.tgId.trim() || null,
      });
      setMethodistForm({
        name: "",
        email: "",
        password: "",
        tgId: "",
      });
      setMessage("Методист успешно добавлен");
      await refreshMethodists();
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось добавить методиста");
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteRow = async (methodist) => {
    setErrorMessage("");
    setMessage("");
    if (!methodist?.id) {
      return;
    }
    const displayName = methodist.name || "(без имени)";
    const ok = window.confirm(`Удалить методиста: ${displayName}?`);
    if (!ok) {
      return;
    }
    setIsLoading(true);
    try {
      await deleteMethodist(methodist.id);
      setMessage("Методист удален");
      await refreshMethodists();
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось удалить методиста");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCloseNotice = () => {
    setMessage("");
    setErrorMessage("");
  };

  return (
      <div className="login-page admin-login role-root role-admin grain-overlay">
        <motion.div
            className="login-content"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
        >
          <div className="admin-title-block glitch" data-text="администрационная панель">
            администрационная панель
          </div>
          <motion.div
              className="admin-card terminal glow-blue"
              initial={{ opacity: 0, y: 24 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.1 }}
          >
            <div className="terminal-header">
              <div className="terminal-dot" style={{ background: "#ff5f57" }} />
              <div className="terminal-dot" style={{ background: "#febc2e" }} />
              <div className="terminal-dot" style={{ background: "#28c840" }} />
              <span className="terminal-title">admin://control</span>
            </div>
            <div className="terminal-body">
              <img src={imgLogo} alt="КУБИК" className="admin-logo" />
              <div className="admin-nav">
                <button type="button" className="admin-nav-btn active" disabled>
                  Методисты
                </button>
                <button
                  type="button"
                  className="admin-nav-btn"
                  onClick={() => navigateTo("/admin/password")}
                >
                  Изменить пароль
                </button>
              </div>

              <motion.form
                className="login-form admin-form"
                onSubmit={handleAddMethodist}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
              >
                  <h2>Добавление методиста</h2>
                  <div className="input-wrapper">
                    <input
                        className="login-input"
                        type="text"
                        value={methodistForm.name}
                        onChange={(event) =>
                            setMethodistForm((prev) => ({
                              ...prev,
                              name: event.target.value,
                            }))
                        }
                        placeholder="ФИО *"
                    />
                  </div>
                  <div className="input-wrapper">
                    <input
                        className="login-input"
                        type="email"
                        value={methodistForm.email}
                        onChange={(event) =>
                            setMethodistForm((prev) => ({
                              ...prev,
                              email: event.target.value,
                            }))
                        }
                        placeholder="Email *"
                    />
                  </div>
                  <div className="input-wrapper">
                    <input
                        className="login-input"
                        type="password"
                        value={methodistForm.password}
                        onChange={(event) =>
                            setMethodistForm((prev) => ({
                              ...prev,
                              password: event.target.value,
                            }))
                        }
                        placeholder="Пароль *"
                    />
                  </div>
                  <div className="input-wrapper">
                    <input
                        className="login-input"
                        type="text"
                        value={methodistForm.tgId}
                        onChange={(event) =>
                            setMethodistForm((prev) => ({
                              ...prev,
                              tgId: event.target.value,
                            }))
                        }
                        placeholder="Telegram ID"
                    />
                  </div>
                  <button className="login-btn" disabled={isLoading}>Сохранить</button>
                </motion.form>

              <div className="admin-methodists">
                <div className="admin-section-title">Список методистов</div>
                <div className="table-card admin-methodists-table">
                  <table>
                    <thead>
                      <tr>
                        <th>ФИО</th>
                        <th>Email</th>
                        <th>Telegram ID</th>
                        <th style={{ width: 120 }}>Действия</th>
                      </tr>
                    </thead>
                    <tbody>
                      {isListLoading ? (
                        <tr>
                          <td colSpan={4} className="admin-empty">
                            Загрузка...
                          </td>
                        </tr>
                      ) : methodists.length === 0 ? (
                        <tr>
                          <td colSpan={4} className="admin-empty">
                            Методисты не найдены
                          </td>
                        </tr>
                      ) : (
                        methodists.map((methodist) => (
                          <tr key={methodist.id}>
                            <td>{methodist.name}</td>
                            <td>{methodist.email}</td>
                            <td>{methodist.tgId || "—"}</td>
                            <td>
                              <button
                                type="button"
                                className="btn btn-sm btn-danger"
                                disabled={isLoading}
                                onClick={() => handleDeleteRow(methodist)}
                              >
                                Удалить
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

            <button className="admin-logout" onClick={onLogout} type="button">Выйти</button>
            {message || errorMessage ? (
                <div className={`admin-notice ${errorMessage ? "admin-notice-error" : "admin-notice-success"}`}>
                  <span>{errorMessage || message}</span>
                  <button type="button" className="admin-notice-close" onClick={handleCloseNotice}>
                    Закрыть
                  </button>
                </div>
            ) : null}
          </div>
        </motion.div>
      </motion.div>
    </div>
  );
}

export default Admin;
