import { useState } from "react";
import { motion } from "framer-motion";
import "./Admin.css";
import imgLogo from "../../images/image.svg";
import { changeOwnAdminPassword } from "../api/adminApi";

function AdminPassword({ onLogout }) {
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [passwordForm, setPasswordForm] = useState({
    password: "",
    confirm: "",
  });

  const navigateTo = (path) => {
    window.history.pushState({}, "", path);
    window.dispatchEvent(new PopStateEvent("popstate"));
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
      await changeOwnAdminPassword(passwordForm.password.trim());
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
              <button
                type="button"
                className="admin-nav-btn"
                onClick={() => navigateTo("/admin")}
              >
                Методисты
              </button>
              <button type="button" className="admin-nav-btn active" disabled>
                Изменить пароль
              </button>
            </div>

            <motion.form
              className="login-form admin-form"
              onSubmit={handlePasswordUpdate}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
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
              <button className="login-btn" disabled={isLoading}>
                Сохранить
              </button>
            </motion.form>

            <button className="admin-logout" onClick={onLogout} type="button">
              Выйти
            </button>

            {message || errorMessage ? (
              <div
                className={`admin-notice ${errorMessage ? "admin-notice-error" : "admin-notice-success"}`}
              >
                <span>{errorMessage || message}</span>
                <button
                  type="button"
                  className="admin-notice-close"
                  onClick={handleCloseNotice}
                >
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

export default AdminPassword;
