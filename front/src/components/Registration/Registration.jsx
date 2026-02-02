import { useState } from "react";
import imgLogo from "../../images/image.png";
import svgPaths, { BackgroundCircles, BackgroundRight, UserInputIcon, LockInputIcon } from "../../svgs/AdminSvg.jsx";
import "./Registration.css";

export default function Registration() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("student");

  // Обработчик отправки формы - переписать сделать свзяь с бэкэндом!
  const handleSubmit = (e) => {
    e.preventDefault();
    alert(`Вход\nРоль: ${role}\nЛогин: ${username}\nПароль: ${password}`);
  };

  return (
      <div className="login-page">
        {/* левые круги */}
        <BackgroundCircles />

        {/* Правая фигура */}
        <BackgroundRight />

        <div className="login-content">
          <img src={imgLogo} alt="КУБИК" className="logo" />
          <form onSubmit={handleSubmit} className="login-form">
            <div className="input-wrapper">
              <select
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                  className="login-select"
                  required
              >
                <option value="methodist">Методист</option>
                <option value="student">Ученик</option>
                <option value="teacher">Преподаватель</option>
                <option value="admin">Админ</option>
              </select>
            </div>
            <div className="input-wrapper">
              {/* реализация человечка */}
              <UserInputIcon svgPaths={svgPaths} />
              <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="USERNAME"
                  className="login-input"
                  required
              />
            </div>
            <div className="input-wrapper">
              {/* реализация замка */}
              <LockInputIcon svgPaths={svgPaths} />
              <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="PASSWORD"
                  className="login-input"
                  required
              />
            </div>
            <button type="submit" className="login-btn">
              Войти
            </button>
          </form>
        </div>
      </div>
  );
}
