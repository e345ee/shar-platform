import { useEffect, useState } from "react";
import "./Admin.css";
import imgLogo from "../../images/image.png";
import {
  changeOwnAdminPassword,
  createMethodist,
  deleteMethodist,
  listMethodists,
} from "../api/adminApi"

function Admin({ onLogout }) {
  const [activeTab, setActiveTab] = useState("add");
  const [methodists, setMethodists] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [methodistForm, setMethodistForm] = useState({
    name: "",
    email: "",
    password: "",
    tgId: "",
  });
  const [removeMethodistId, setRemoveMethodistId] = useState("");
  const [passwordForm, setPasswordForm] = useState({
    password: "",
    confirm: "",
  });

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const rows = await listMethodists();
        if (!cancelled) {
          setMethodists(rows);
        }
      } catch (e) {
        if (!cancelled) {
          setMethodists([]);
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (methodists.length === 0) {
      setRemoveMethodistId("");
      return;
    }
    if (!methodists.find((methodist) => methodist.id === Number(removeMethodistId))) {
      setRemoveMethodistId(String(methodists[0].id));
    }
  }, [methodists, removeMethodistId]);

  const refreshMethodists = async () => {
    const rows = await listMethodists();
    setMethodists(rows);
  };

  useEffect(() => {
    if (activeTab !== "remove") {
      return;
    }
    let cancelled = false;
    const loadForRemoveTab = async () => {
      try {
        const rows = await listMethodists();
        if (!cancelled) {
          setMethodists(rows);
        }
      } catch (e) {
        if (!cancelled) {
          setMethodists([]);
        }
      }
    };
    loadForRemoveTab();
    return () => {
      cancelled = true;
    };
  }, [activeTab]);

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
      const created = await createMethodist({
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
      if (created?.id != null) {
        setRemoveMethodistId(String(created.id));
      }
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось добавить методиста");
    } finally {
      setIsLoading(false);
    }
  };

  const handleRemoveMethodist = async (event) => {
    event.preventDefault();
    setErrorMessage("");
    setMessage("");
    if (!removeMethodistId) {
      setErrorMessage("Выберите методиста");
      return;
    }
    setIsLoading(true);
    try {
      await deleteMethodist(removeMethodistId);
      setMessage("Методист удален");
      await refreshMethodists();
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось удалить методиста");
    } finally {
      setIsLoading(false);
    }
  };

  const handlePasswordUpdate = async (event) => {
    event.preventDefault();
    setErrorMessage("");
    setMessage("");
    if (!passwordForm.password.trim()) {
      setErrorMessage("Введите новый пароль");
      return;
    }
    if (passwordForm.password !== passwordForm.confirm) {
      setErrorMessage("Пароли не совпадают");
      return;
    }
    setIsLoading(true);
    try {
      await changeOwnAdminPassword(passwordForm.password);
      setPasswordForm({ password: "", confirm: "" });
      setMessage("Пароль успешно изменен");
    } catch (e) {
      setErrorMessage(e?.message || "Не удалось изменить пароль");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCloseNotice = () => {
    setMessage("");
    setErrorMessage("");
  };

  return (
      <div className="login-page admin-login">
        <div className="login-content">
          <div className="admin-title-block">администрационная панель</div>
          <div className="admin-card">
            <img src={imgLogo} alt="КУБИК" className="admin-logo" />
            <div className="admin-tabs">
              <button
                  className={`admin-tab ${activeTab === "add" ? "active" : ""}`}
                  onClick={() => setActiveTab("add")}
              >
                Добавить методиста
              </button>
              <button
                  className={`admin-tab ${activeTab === "remove" ? "active" : ""}`}
                  onClick={() => setActiveTab("remove")}
              >
                Удалить методиста
              </button>
              <button
                  className={`admin-tab ${activeTab === "password" ? "active" : ""}`}
                  onClick={() => setActiveTab("password")}
              >
                Изменить пароль
              </button>
            </div>

            {activeTab === "add" && (
                <form
                    className="login-form admin-form"
                    onSubmit={handleAddMethodist}
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
                </form>
            )}

            {activeTab === "remove" && (
                <form
                    className="login-form admin-form"
                    onSubmit={handleRemoveMethodist}
                >
                  <h2>Удаление методиста</h2>
                  <div className="input-wrapper">
                    <select
                        className="login-select"
                        value={removeMethodistId}
                        onChange={(event) =>
                            setRemoveMethodistId(event.target.value)
                        }
                    >
                      {methodists.map((methodist) => (
                          <option key={methodist.id} value={String(methodist.id)}>
                            {methodist.name}
                          </option>
                      ))}
                    </select>
                  </div>
                  <button className="login-btn" disabled={isLoading || methodists.length === 0}>Удалить</button>
                </form>
            )}

            {activeTab === "password" && (
                <form
                    className="login-form admin-form"
                    onSubmit={handlePasswordUpdate}
                >
                  <h2>Изменение пароля</h2>
                  <div className="input-wrapper">
                    <input
                        className="login-input"
                        type="password"
                        value={passwordForm.password}
                        onChange={(event) =>
                            setPasswordForm((prev) => ({
                              ...prev,
                              password: event.target.value,
                            }))
                        }
                        placeholder="Новый пароль *"
                    />
                  </div>
                  <div className="input-wrapper">
                    <input
                        className="login-input"
                        type="password"
                        value={passwordForm.confirm}
                        onChange={(event) =>
                            setPasswordForm((prev) => ({
                              ...prev,
                              confirm: event.target.value,
                            }))
                        }
                        placeholder="Повтор пароля *"
                    />
                  </div>
                  <button className="login-btn" disabled={isLoading}>Сохранить</button>
                </form>
            )}
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
        </div>
      </div>
  );
}

export default Admin;
