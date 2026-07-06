const AUTH_TOKEN_KEY = "auth_access_token";

function getAccessToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY) || "";
}

function errorFromResponse(response, fallbackMessage) {
    const error = new Error(fallbackMessage);
    error.status = response?.status;
    return error;
}

export async function fetchAuthedBlob(url) {
    if (!url) {
        throw new Error("URL is required");
    }

    const token = getAccessToken();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};

    const response = await fetch(url, {
        method: "GET",
        headers,
    });

    if (!response.ok) {
        throw errorFromResponse(response, `Request failed (HTTP ${response.status})`);
    }

    return response.blob();
}

export async function fetchAuthedObjectUrl(url) {
    const blob = await fetchAuthedBlob(url);
    return URL.createObjectURL(blob);
}

export function revokeObjectUrl(objectUrl) {
    if (typeof objectUrl === "string" && objectUrl.startsWith("blob:")) {
        URL.revokeObjectURL(objectUrl);
    }
}

export async function openAuthedInNewTab(url) {
    const objectUrl = await fetchAuthedObjectUrl(url);

    window.open(objectUrl, "_blank", "noopener,noreferrer");
    return objectUrl;
}
