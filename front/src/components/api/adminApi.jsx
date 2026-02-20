const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";
const AUTH_TOKEN_KEY = "auth_access_token";

function getAccessToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY) || "";
}

async function requestJson(path, { method = "GET", body } = {}) {
    const token = getAccessToken();
    const headers = {
        Accept: "application/json",
    };
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }
    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (response.status === 204) {
        return null;
    }

    const text = await response.text();
    let payload = null;
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch (e) {
            payload = null;
        }
    }

    if (!response.ok) {
        const error = new Error(payload?.message || "Request failed");
        error.status = response.status;
        error.payload = payload;
        throw error;
    }

    return payload;
}

export async function listMethodists() {
    const users = await requestJson("/api/users/methodists");
    if (!Array.isArray(users)) {
        return [];
    }
    return users
        .map((u) => ({ id: u.id, name: u.name, email: u.email }));
}

export function createMethodist(dto) {
    return requestJson("/api/users/methodists", {
        method: "POST",
        body: dto,
    });
}

export function deleteMethodist(methodistId) {
    return requestJson(`/api/users/methodists/${methodistId}`, {
        method: "DELETE",
    });
}

export function changeOwnAdminPassword(newPassword) {
    return requestJson("/api/users/admin/password", {
        method: "PUT",
        body: { newPassword },
    });
}
