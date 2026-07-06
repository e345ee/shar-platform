import Admin from "../Admin/Admin";

export default function AdminPage({ onLogout }) {
    return (
        <div className="admin-page-wrapper">
            <Admin onLogout={onLogout} />
        </div>
    );
}
