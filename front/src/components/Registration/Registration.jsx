import { useState } from "react";
import imgLogo from "../../images/image.png";
import svgPaths, { BackgroundCircles, BackgroundRight, UserInputIcon, LockInputIcon } from "../../svgs/AdminSvg.jsx";
import "./Registration.css";

export default function Registration({ onLogin, isLoading, errorMessage }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    await onLogin(username.trim(), password);
  };

  return (
      <div className="registration-login-page">
        {/* левые круги */}
        <BackgroundCircles />

        {/* Правая фигура */}
        <BackgroundRight />

        <div className="login-content">
          <img src={imgLogo} alt="КУБИК" className="logo" />
          <form onSubmit={handleSubmit} className="login-form">
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
            <button type="submit" className="login-btn" disabled={isLoading}>
              {isLoading ? "Вход..." : "Войти"}
            </button>
            {errorMessage ? <p className="login-error">{errorMessage}</p> : null}
          </form>
        </div>
      </div>
  );
}
