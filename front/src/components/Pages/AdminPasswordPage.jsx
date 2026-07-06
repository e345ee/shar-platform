import AdminPassword from "../Admin/AdminPassword";

export default function AdminPasswordPage({ onLogout }) {
  return (
    <div className="admin-page-wrapper">
      <AdminPassword onLogout={onLogout} />
    </div>
  );
}
