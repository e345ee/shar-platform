import { useState } from "react";
import imgLogo from "../../images/image.png";
import svgPaths from "../../svgs/svg.js";
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
      <svg className="bg-circles" fill="none" viewBox="0 0 1641.5 1083">
        <circle cx="362" cy="721" r="362" fill="#283541" />
        <circle cx="362" cy="721" r="286" fill="#3A4C5D" />
        <circle cx="362" cy="721" r="219" fill="#4C6275" />
      </svg>

      {/* Правая фигура */}
      <svg className="bg-right" fill="none" viewBox="0 0 864 722">
        <path
          d="M257 140C121.4 152.8 29.1667 52 0 0L864 3.5V721.5H767.5C468.7 683.5 519 532 581.5 461C618 411.167 684 291.1 656 209.5C621 107.5 426.5 124 257 140Z"
          fill="#283541"
        />
      </svg>

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
            <svg className="input-icon" fill="none" viewBox="0 0 20 20">
              <path
                d={svgPaths.p27365a00}
                stroke="white"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <path
                d={svgPaths.p6f5b580}
                stroke="white"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
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
            <svg className="input-icon" fill="none" viewBox="0 0 20 20">
              <path
                d={svgPaths.p2566d000}
                stroke="white"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <path
                d={svgPaths.p1bf79e00}
                stroke="white"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
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
