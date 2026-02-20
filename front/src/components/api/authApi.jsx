const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";

async function requestJson(path, options = {}) {
    const response = await fetch(`${API_BASE_URL}${path}`, options);
    const text = await response.text();
    let body = null;

    if (text) {
        try {
            body = JSON.parse(text);
        } catch (e) {
            body = null;
        }
    }

    if (!response.ok) {
        const backendMessage = body?.message || body?.error || "";
        const error = new Error(
            backendMessage
                ? `${backendMessage} (HTTP ${response.status})`
                : `Request failed (HTTP ${response.status})`,
        );
        error.status = response.status;
        error.body = body;
        throw error;
    }

    return body;
}

export async function login(username, password) {
    return requestJson("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
    });
}

export async function getMe(accessToken) {
    return requestJson("/api/me", {
        method: "GET",
        headers: { Authorization: `Bearer ${accessToken}` },
    });
}

export async function getRoleById(roleId, accessToken) {
    return requestJson(`/api/roles/${roleId}`, {
        method: "GET",
        headers: { Authorization: `Bearer ${accessToken}` },
    });
}

export async function authenticateUser(username, password) {
    try {
        const auth = await login(username, password);
        const accessToken = auth?.accessToken;
        if (!accessToken) {
            const e = new Error("Не удалось получить access token");
            e.code = "AUTH_TOKEN_MISSING";
            throw e;
        }

        const me = await getMe(accessToken);
        const role = await getRoleById(me?.roleId, accessToken);

        return {
            accessToken,
            user: me,
            roleName: role?.rolename || "",
        };
    } catch (error) {
        if (error?.status === 401 || error?.status === 403) {
            const e = new Error("Неправильный логин или пароль");
            e.code = "INVALID_CREDENTIALS";
            throw e;
        }
        throw error;
    }
}
