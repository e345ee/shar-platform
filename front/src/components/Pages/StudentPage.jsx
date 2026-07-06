import Student from "../Student/Student";

export default function StudentPage({ onLogin, isLoading, errorMessage }) {
    return (
        <div className="registration-page-wrapper">
            <Student onLogin={onLogin} isLoading={isLoading} errorMessage={errorMessage} />
        </div>
    );
}
