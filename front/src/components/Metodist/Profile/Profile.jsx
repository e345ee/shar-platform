import { useEffect, useState } from "react";
import "./Profile.css";
import {
    HomeIcon,
    UserIcon,
    EmailIcon,
    LockIcon,
    MessageIcon,
    TelegramIcon,
} from "../../../svgs/ProfileSvg";
import { deleteMyAvatar, getMyProfile, updateMyProfile, uploadMyAvatar } from "../../api/methodistApi";

function Profile({ onBackToMain }) {
    const [isEditing, setIsEditing] = useState(false);
    const [profile, setProfile] = useState({
        name: "",
        email: "",
        password: "",
        bio: "",
        tgId: "",
        photo: "",
    });
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [isPhotoUploading, setIsPhotoUploading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    const loadProfile = async () => {
        setIsLoading(true);
        setErrorMessage("");
        try {
            const me = await getMyProfile();
            setProfile({
                name: me?.name || "",
                email: me?.email || "",
                password: "",
                bio: me?.bio || "",
                tgId: me?.tgId || "",
                photo: me?.photo || "",
            });
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось загрузить профиль");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadProfile();
    }, []);

    const handleInputChange = (field, value) => {
        setProfile((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSave = async () => {
        setIsSaving(true);
        setErrorMessage("");
        setSuccessMessage("");
        try {
            const dto = {
                name: profile.name.trim(),
                email: profile.email.trim(),
                bio: profile.bio,
                tgId: profile.tgId.trim(),
            };
            if (profile.password.trim()) {
                dto.password = profile.password;
            }
            await updateMyProfile(dto);
            await loadProfile();
            setProfile((prev) => ({ ...prev, password: "" }));
            setIsEditing(false);
            setSuccessMessage("Профиль сохранен");
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось сохранить профиль");
        } finally {
            setIsSaving(false);
        }
    };

    const handleChoosePhoto = async (e) => {
        const file = e.target.files?.[0] || null;
        if (!file) {
            return;
        }
        setIsPhotoUploading(true);
        setErrorMessage("");
        setSuccessMessage("");
        try {
            const updated = await uploadMyAvatar(file);
            setProfile((prev) => ({ ...prev, photo: updated?.photo || prev.photo }));
            setSuccessMessage("Фото профиля обновлено");
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось загрузить фото");
        } finally {
            setIsPhotoUploading(false);
            e.target.value = "";
        }
    };

    const handleDeletePhoto = async () => {
        setErrorMessage("");
        setSuccessMessage("");
        setIsPhotoUploading(true);
        try {
            const updated = await deleteMyAvatar();
            setProfile((prev) => ({ ...prev, photo: updated?.photo || "" }));
            setSuccessMessage("Фото профиля удалено");
        } catch (error) {
            setErrorMessage(error?.message || "Не удалось удалить фото");
        } finally {
            setIsPhotoUploading(false);
        }
    };

    return (
        <div className="profile-page">
            <div className="profile-container">
                <div className="profile-card">
                    <header className="profile-header">
                        <div className="profile-header-center">
                            <h1 className="profile-header-title">Профиль методиста</h1>
                        </div>
                        <div className="profile-header-actions">
                            <button className="btn-secondary" onClick={onBackToMain} type="button">
                                <HomeIcon />
                                На главную
                            </button>
                        </div>
                    </header>

                    <main className="profile-main">
                        {errorMessage && <div className="profile-error">{errorMessage}</div>}
                        {successMessage && <div className="profile-success">{successMessage}</div>}
                        {isLoading && <div className="profile-success">Загрузка профиля...</div>}

                        <div className="profile-photo-section">
                            <label className="profile-photo-upload profile-photo-wrapper" htmlFor="profile-photo-input">
                                {profile.photo ? (
                                    <img
                                        src={profile.photo}
                                        alt="Профиль"
                                        className="profile-photo"
                                    />
                                ) : (
                                    <div className="profile-photo profile-photo-empty" aria-label="Фото не загружено">
                                        <UserIcon />
                                    </div>
                                )}
                                <span className="profile-photo-hint">
                                    {isPhotoUploading ? "Загрузка..." : "Нажмите, чтобы загрузить фото"}
                                </span>
                            </label>
                            <input
                                id="profile-photo-input"
                                className="profile-photo-input"
                                type="file"
                                accept="image/*"
                                onChange={handleChoosePhoto}
                                disabled={isPhotoUploading}
                            />
                            <div className="profile-photo-info">
                                <label className="profile-label">Фото профиля </label>
                                <div className="profile-url">{profile.photo ? "Фото загружено" : "Фото не загружено"}</div>
                                {isEditing && (
                                    <div className="profile-photo-actions">
                                        <button className="btn-secondary" type="button" onClick={handleDeletePhoto}>
                                            Удалить фото
                                        </button>
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
                                            onChange={(e) => handleInputChange("name", e.target.value)}
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
                                            onChange={(e) => handleInputChange("email", e.target.value)}
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
                                            placeholder="Оставьте пустым, если не нужно менять"
                                            value={profile.password}
                                            onChange={(e) => handleInputChange("password", e.target.value)}
                                        />
                                    ) : (
                                        <div className="profile-value">********</div>
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
                                            onChange={(e) => handleInputChange("bio", e.target.value)}
                                            rows="4"
                                        />
                                    ) : (
                                        <div className="profile-value">{profile.bio || "нет информации"}</div>
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
                                            onChange={(e) => handleInputChange("tgId", e.target.value)}
                                        />
                                    ) : (
                                        <div className="profile-value">{profile.tgId || "Не указан"}</div>
                                    )}
                                </div>
                            </div>
                        </div>

                        <div className="profile-actions">
                            {isEditing ? (
                                <button className="btn-primary" onClick={handleSave} type="button" disabled={isSaving}>
                                    Сохранить изменения
                                </button>
                            ) : (
                                <button className="btn-primary" onClick={() => setIsEditing(true)} type="button">
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

export default Profile;
