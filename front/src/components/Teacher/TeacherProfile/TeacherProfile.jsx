import { useState, useEffect } from "react";
import "./TeacherProfile.css";
import {
  HomeIcon,
  UserIcon,
  EmailIcon,
  LockIcon,
  MessageIcon,
  TelegramIcon,
} from "../../../svgs/ProfileSvg.jsx";
import {
  getMyProfile,
  updateMyProfile,
  uploadMyAvatar,
} from "../../api/teacherApi";

function TeacherProfile({ onBackToMain, onLogout }) {
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [profile, setProfile] = useState({
    name: "",
    email: "",
    password: "",
    bio: "",
    tgId: "",
    photo: "",
  });

  useEffect(() => {
    const loadProfile = async () => {
      setIsLoading(true);
      setErrorMessage("");
      try {
        const data = await getMyProfile();
        setProfile({
          name: data.name || "",
          email: data.email || "",
          password: "",
          bio: data.bio || "",
          tgId: data.tgId || "",
          photo: data.photo || "",
        });
      } catch (error) {
        setErrorMessage(error?.message || "Не удалось загрузить профиль");
        console.error("Ошибка загрузки профиля:", error);
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, []);

  const handleInputChange = (field, value) => {
    setProfile((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsSaving(true);
    setErrorMessage("");
    try {
      const updated = await uploadMyAvatar(file);
      setProfile((prev) => ({
        ...prev,
        photo: updated.photo || prev.photo,
      }));
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось загрузить фото");
    } finally {
      setIsSaving(false);
    }
  };

  const handleSave = async () => {
    setIsSaving(true);
    setErrorMessage("");
    try {
      const updateData = {};
      if (profile.name) updateData.name = profile.name;
      if (profile.email) updateData.email = profile.email;
      if (profile.bio) updateData.bio = profile.bio;
      if (profile.tgId) updateData.tgId = profile.tgId;
      if (profile.photo) updateData.photo = profile.photo;
      // Пароль обновляется отдельно через changePassword

      const updated = await updateMyProfile(updateData);
      setProfile((prev) => ({
        ...prev,
        name: updated.name || prev.name,
        email: updated.email || prev.email,
        bio: updated.bio || prev.bio,
        tgId: updated.tgId || prev.tgId,
        photo: updated.photo || prev.photo,
        password: "", // Очищаем пароль после сохранения
      }));
      setIsEditing(false);
    } catch (error) {
      setErrorMessage(error?.message || "Не удалось сохранить изменения");
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancel = () => {
    setIsEditing(false);
    // Перезагружаем профиль для отмены изменений
    const loadProfile = async () => {
      try {
        const data = await getMyProfile();
        setProfile({
          name: data.name || "",
          email: data.email || "",
          password: "",
          bio: data.bio || "",
          tgId: data.tgId || "",
          photo: data.photo || "",
        });
      } catch (error) {
        console.error("Ошибка загрузки профиля:", error);
      }
    };
    loadProfile();
  };

  if (isLoading) {
    return (
        <div className="profile-page">
          <div className="profile-container">
            <div className="profile-card">
              <div className="profile-loading">Загрузка профиля...</div>
            </div>
          </div>
        </div>
    );
  }

  return (
      <div className="profile-page">
        <div className="profile-container">
          <div className="profile-card">
            <header className="profile-header">
              <div className="profile-header-center">
                <h1 className="profile-header-title">Профиль преподавателя</h1>
              </div>
              <div className="profile-header-actions">
                <button
                    className="btn-secondary"
                    onClick={onBackToMain}
                    type="button"
                >
                  <HomeIcon />
                  На главную
                </button>
              </div>
            </header>

            {errorMessage && (
                <div className="profile-error">{errorMessage}</div>
            )}

            <main className="profile-main">
              <div className="profile-photo-section">
                <div className="profile-photo-wrapper">
                  {profile.photo ? (
                      <img
                          src={profile.photo}
                          alt="Профиль"
                          className="profile-photo"
                          onError={(e) => {
                            e.target.src = "https://via.placeholder.com/200";
                          }}
                      />
                  ) : (
                      <div className="profile-photo-placeholder">
                        <UserIcon />
                      </div>
                  )}
                </div>
                <div className="profile-photo-info">
                  <label className="profile-label">Фото профиля</label>
                  {isEditing ? (
                      <div>
                        <input
                            type="file"
                            accept="image/*"
                            onChange={handleFileChange}
                            className="profile-file-input"
                            disabled={isSaving}
                        />
                        {profile.photo && (
                            <div className="profile-url">{profile.photo}</div>
                        )}
                      </div>
                  ) : (
                      <div className="profile-url">
                        {profile.photo || "Фото не загружено"}
                      </div>
                  )}
                </div>
              </div>

              <div className="profile-form">
                <div className="profile-field">
                  <div className="profile-field-icon">
                    <UserIcon />
                  </div>
                  <div className="profile-field-content">
                    <label className="profile-label">Имя</label>
                    {isEditing ? (
                        <input
                            type="text"
                            className="profile-input"
                            value={profile.name}
                            onChange={(e) =>
                                handleInputChange("name", e.target.value)
                            }
                        />
                    ) : (
                        <div className="profile-value">{profile.name}</div>
                    )}
                  </div>
                </div>

                <div className="profile-field">
                  <div className="profile-field-icon">
                    <EmailIcon />
                  </div>
                  <div className="profile-field-content">
                    <label className="profile-label">Email</label>
                    {isEditing ? (
                        <input
                            type="email"
                            className="profile-input"
                            value={profile.email}
                            onChange={(e) =>
                                handleInputChange("email", e.target.value)
                            }
                        />
                    ) : (
                        <div className="profile-value">{profile.email}</div>
                    )}
                  </div>
                </div>

                <div className="profile-field">
                  <div className="profile-field-icon">
                    <LockIcon />
                  </div>
                  <div className="profile-field-content">
                    <label className="profile-label">Пароль</label>
                    {isEditing ? (
                        <input
                            type="password"
                            className="profile-input"
                            value={profile.password}
                            onChange={(e) =>
                                handleInputChange("password", e.target.value)
                            }
                        />
                    ) : (
                        <div className="profile-value">{profile.password}</div>
                    )}
                  </div>
                </div>

                <div className="profile-field">
                  <div className="profile-field-icon">
                    <MessageIcon />
                  </div>
                  <div className="profile-field-content">
                    <label className="profile-label">О себе</label>
                    {isEditing ? (
                        <textarea
                            className="profile-textarea"
                            value={profile.bio}
                            onChange={(e) =>
                                handleInputChange("bio", e.target.value)
                            }
                            rows="4"
                            placeholder="Расскажите о себе"
                        />
                    ) : (
                        <div className="profile-value">
                          {profile.bio || "Не указано"}
                        </div>
                    )}
                  </div>
                </div>

                <div className="profile-field">
                  <div className="profile-field-icon">
                    <TelegramIcon />
                  </div>
                  <div className="profile-field-content">
                    <label className="profile-label">Telegram ID</label>
                    {isEditing ? (
                        <input
                            type="text"
                            className="profile-input"
                            value={profile.tgId}
                            onChange={(e) =>
                                handleInputChange("tgId", e.target.value)
                            }
                            placeholder="@username"
                        />
                    ) : (
                        <div className="profile-value">
                          {profile.tgId || "Не указано"}
                        </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="profile-actions">
                {isEditing ? (
                    <>
                      <button
                          className="btn-secondary"
                          onClick={handleCancel}
                          type="button"
                          disabled={isSaving}
                      >
                        Отмена
                      </button>
                      <button
                          className="btn-primary"
                          onClick={handleSave}
                          type="button"
                          disabled={isSaving}
                      >
                        {isSaving ? "Сохранение..." : "Сохранить изменения"}
                      </button>
                    </>
                ) : (
                    <button
                        className="btn-primary"
                        onClick={() => setIsEditing(true)}
                        type="button"
                    >
                      Редактировать профиль
                    </button>
                )}
              </div>
            </main>
          </div>
        </div>
      </div>
  );
}

export default TeacherProfile;
