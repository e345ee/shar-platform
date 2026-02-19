import { useEffect, useMemo, useState } from "react";
import RegistrationPage from "./components/Pages/RegistrationPage";
import AdminPage from "./components/Pages/AdminPage";
import Methodist from "./components/Metodist/Metodist";
import Student from "./components/Student/Student";
import Teacher from "./components/Teacher/Teacher";
import { authenticateUser } from "./components/api/authApi";
import StudentPage from "./components/Pages/StudentPage";

const AUTH_TOKEN_KEY = "auth_access_token";
const AUTH_ROLE_KEY = "auth_role_name";

function routeByRole(roleName) {
  const role = (roleName || "").toUpperCase();
  if (role === "ADMIN") return "/admin";
  if (role === "METHODIST") return "/methodist";
  if (role === "STUDENT") return "/student";
  if (role === "TEACHER") return "/teacher";
  return "/unsupported";
}

function App() {
  const [authToken, setAuthToken] = useState(
      () => localStorage.getItem(AUTH_TOKEN_KEY) || "",
  );
  const [roleName, setRoleName] = useState(
      () => localStorage.getItem(AUTH_ROLE_KEY) || "",
  );
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [currentPath, setCurrentPath] = useState(
      () => window.location.pathname || "/",
  );

  const normalizedRole = useMemo(
      () => (roleName || "").toUpperCase(),
      [roleName],
  );

  useEffect(() => {
    const onPopState = () => setCurrentPath(window.location.pathname || "/");
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  const navigate = (path, replace = false) => {
    if (replace) {
      window.history.replaceState({}, "", path);
    } else {
      window.history.pushState({}, "", path);
    }
    setCurrentPath(path);
  };

  useEffect(() => {
    if (!authToken) {
      if (currentPath !== "/login" && currentPath !== "/") {
        navigate("/login", true);
      }
      return;
    }
    if (currentPath === "/" || currentPath === "/login") {
      navigate(routeByRole(roleName), true);
    }
  }, [authToken, roleName, currentPath]);

  const handleLogin = async (username, password) => {
    setErrorMessage("");
    setIsLoading(true);
    try {
      const auth = await authenticateUser(username, password);
      localStorage.setItem(AUTH_TOKEN_KEY, auth.accessToken);
      localStorage.setItem(AUTH_ROLE_KEY, auth.roleName || "");
      setAuthToken(auth.accessToken);
      setRoleName(auth.roleName || "");
      navigate(routeByRole(auth.roleName), true);
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось выполнить вход");
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_ROLE_KEY);
    setAuthToken("");
    setRoleName("");
    setErrorMessage("");
    navigate("/login", true);
  };

  if (!authToken || currentPath === "/login" || currentPath === "/") {
    return (
        <RegistrationPage
            onLogin={handleLogin}
            isLoading={isLoading}
            errorMessage={errorMessage}
        />
    );
  }

  if (currentPath === "/admin" && normalizedRole === "ADMIN") {
    return <AdminPage onLogout={handleLogout} />;
  }

  if (currentPath === "/methodist" && normalizedRole === "METHODIST") {
    return <Methodist onLogout={handleLogout} />;
  }

  if (currentPath === "/student" && normalizedRole === "STUDENT") {
    return <Student onLogout={handleLogout} />;
  }

  if (currentPath === "/teacher" && normalizedRole === "TEACHER") {
    return <Teacher onLogout={handleLogout} />;
  }

  return (
      <div
          style={{
            minHeight: "100vh",
            display: "grid",
            placeItems: "center",
            background: "#445a6d",
            color: "#fff",
          }}
      >
        <div style={{ textAlign: "center" }}>
          <h2>
            Роль {normalizedRole || "UNKNOWN"} пока не поддерживается на фронте
          </h2>
          <button
              onClick={handleLogout}
              style={{ height: 44, padding: "0 18px", cursor: "pointer" }}
          >
            Выйти
          </button>
        </div>
      </div>
  );
}

export default App;
