import { useState } from "react";
import "./Profile.css";
import {
    HomeIcon,
    UserIcon,
    EmailIcon,
    LockIcon,
    MessageIcon,
    TelegramIcon,
} from "../../../svgs/ProfileSvg";

function Profile({ onBackToMain, onLogout }) {
    const [isEditing, setIsEditing] = useState(false);
    const [profile, setProfile] = useState({
        name: "Иван Петров",
        email: "ivan.petrov@example.com",
        password: "********",
        about: "Методист с 10-летним стажем работы в образовательной сфере. Специализируюсь на разработке учебных программ и методических материалов.",
        telegramId: "@ivan_methodist",
        photoUrl: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&h=400&fit=crop",
    });

    const handleInputChange = (field, value) => {
        setProfile((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSave = () => {
        setIsEditing(false);
        // Здесь можно добавить логику сохранения
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
                        <div className="profile-photo-section">
                            <div className="profile-photo-wrapper">
                                <img
                                    src={profile.photoUrl}
                                    alt="Профиль"
                                    className="profile-photo"
                                />
                            </div>
                            <div className="profile-photo-info">
                                <label className="profile-label">Фото профиля</label>
                                {isEditing ? (
                                    <input
                                        type="text"
                                        className="profile-input"
                                        value={profile.photoUrl}
                                        onChange={(e) => handleInputChange("photoUrl", e.target.value)}
                                    />
                                ) : (
                                    <div className="profile-url">{profile.photoUrl}</div>
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
                                            value={profile.password}
                                            onChange={(e) => handleInputChange("password", e.target.value)}
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
                                            value={profile.about}
                                            onChange={(e) => handleInputChange("about", e.target.value)}
                                            rows="4"
                                        />
                                    ) : (
                                        <div className="profile-value">{profile.about}</div>
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
                                            value={profile.telegramId}
                                            onChange={(e) => handleInputChange("telegramId", e.target.value)}
                                        />
                                    ) : (
                                        <div className="profile-value">{profile.telegramId}</div>
                                    )}
                                </div>
                            </div>
                        </div>

                        <div className="profile-actions">
                            {isEditing ? (
                                <button className="btn-primary" onClick={handleSave} type="button">
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
