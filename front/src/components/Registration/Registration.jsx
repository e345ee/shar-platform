import { useState } from "react";
import { motion } from "framer-motion";
import imgLogo from "../../images/image.svg";
import svgPaths, {
  BackgroundCircles,
  BackgroundRight,
  UserInputIcon,
  LockInputIcon,
} from "../../svgs/AdminSvg.jsx";
import "./Registration.css";

export default function Registration({ onLogin, isLoading, errorMessage }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    await onLogin(username.trim(), password);
  };

  return (
    <div className="registration-login-page role-root role-auth grain-overlay">
      <BackgroundCircles />
      <BackgroundRight />

      <motion.div
        className="auth-hero"
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
      >
        <h1 className="auth-title glitch" data-text="SHAR PLATFORM">
          SHAR PLATFORM
        </h1>
        <p className="auth-subtitle">
          One entry point. Every role. One place to focus.
        </p>
      </motion.div>

      <motion.div
        className="auth-terminal terminal glow-blue"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, delay: 0.1 }}
      >
        <div className="terminal-header">
          <div className="terminal-dot" style={{ background: "#ff5f57" }} />
          <div className="terminal-dot" style={{ background: "#febc2e" }} />
          <div className="terminal-dot" style={{ background: "#28c840" }} />
          <span className="terminal-title">auth://login</span>
        </div>
        <div className="terminal-body">
          <img src={imgLogo} alt="KUBIK" className="logo" />
          <form onSubmit={handleSubmit} className="login-form">
            <div className="input-wrapper">
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
              {isLoading ? "Signing in..." : "Sign in"}
            </button>
            {errorMessage ? <p className="login-error">{errorMessage}</p> : null}
          </form>
        </div>
      </motion.div>
    </div>
  );
}
