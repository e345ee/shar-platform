import Registration from "../Registration/Registration";

export default function RegistrationPage({ onLogin, isLoading, errorMessage }) {
    return (
        <div className="registration-page-wrapper">
            <Registration onLogin={onLogin} isLoading={isLoading} errorMessage={errorMessage} />
        </div>
    );
}
