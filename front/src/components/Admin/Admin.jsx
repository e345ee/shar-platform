import { useEffect, useState } from "react";
import "./Admin.css";
import imgLogo from "../../images/image.png";

const initialMethodists = [
  {
    id: 1,
    name: "Ирина Шевченко",
    email: "irina@skillup.ru",
  },
  {
    id: 2,
    name: "Антон Крылов",
    email: "anton@skillup.ru",
  },
  {
    id: 3,
    name: "Мария Фролова",
    email: "maria@skillup.ru",
  },
];

function Admin() {
  const [activeTab, setActiveTab] = useState("add");
  const [methodists, setMethodists] = useState(initialMethodists);
  const [methodistForm, setMethodistForm] = useState({
    name: "",
    email: "",
    password: "",
    tgId: "",
  });
  const [removeMethodistId, setRemoveMethodistId] = useState(
    initialMethodists[0]?.id ?? "",
  );
  const [passwordForm, setPasswordForm] = useState({
    methodistId: initialMethodists[0]?.id ?? "",
    password: "",
    confirm: "",
  });

  useEffect(() => {
    if (methodists.length === 0) {
      setRemoveMethodistId("");
      setPasswordForm((prev) => ({ ...prev, methodistId: "" }));
      return;
    }
    if (!methodists.find((methodist) => methodist.id === removeMethodistId)) {
      setRemoveMethodistId(methodists[0].id);
    }
    if (
      !methodists.find((methodist) => methodist.id === passwordForm.methodistId)
    ) {
      setPasswordForm((prev) => ({ ...prev, methodistId: methodists[0].id }));
    }
  }, [methodists, removeMethodistId, passwordForm.methodistId]);

  const handleAddMethodist = (event) => {
    event.preventDefault();
    if (
      !methodistForm.name.trim() ||
      !methodistForm.email.trim() ||
      !methodistForm.password.trim()
    ) {
      return;
    }
    const newMethodist = {
      id: Date.now(),
      name: methodistForm.name.trim(),
      email: methodistForm.email.trim(),
      password: methodistForm.password.trim(),
      tgId: methodistForm.tgId.trim(),
    };
    setMethodists((prev) => [...prev, newMethodist]);
    setMethodistForm({
      name: "",
      email: "",
      password: "",
      tgId: "",
    });
    setRemoveMethodistId(newMethodist.id);
  };

  const handleRemoveMethodist = (event) => {
    event.preventDefault();
    if (!removeMethodistId) {
      return;
    }
    setMethodists((prev) =>
      prev.filter((methodist) => methodist.id !== removeMethodistId),
    );
  };

  const handlePasswordUpdate = (event) => {
    event.preventDefault();
    if (!passwordForm.methodistId || !passwordForm.password.trim()) {
      return;
    }
    if (passwordForm.password !== passwordForm.confirm) {
      return;
    }
    setPasswordForm((prev) => ({ ...prev, password: "", confirm: "" }));
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
              <button className="login-btn">Сохранить</button>
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
                    setRemoveMethodistId(Number(event.target.value))
                  }
                >
                  {methodists.map((methodist) => (
                    <option key={methodist.id} value={methodist.id}>
                      {methodist.name}
                    </option>
                  ))}
                </select>
              </div>
              <button className="login-btn">Удалить</button>
            </form>
          )}

          {activeTab === "password" && (
            <form
              className="login-form admin-form"
              onSubmit={handlePasswordUpdate}
            >
              <h2>Изменение пароля</h2>
              <div className="input-wrapper">
                <select
                  className="login-select"
                  value={passwordForm.methodistId}
                  onChange={(event) =>
                    setPasswordForm((prev) => ({
                      ...prev,
                      methodistId: Number(event.target.value),
                    }))
                  }
                >
                  {methodists.map((methodist) => (
                    <option key={methodist.id} value={methodist.id}>
                      {methodist.name}
                    </option>
                  ))}
                </select>
              </div>
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
              <button className="login-btn">Сохранить</button>
            </form>
          )}
          <button className="admin-logout">Выйти</button>
        </div>
      </div>
    </div>
  );
}

export default Admin;
