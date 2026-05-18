const AuthApi = (() => {
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    function readCookie(name) {
        const encodedName = `${name}=`;
        return document.cookie
            .split(";")
            .map((item) => item.trim())
            .find((item) => item.startsWith(encodedName))
            ?.substring(encodedName.length) ?? null;
    }

    async function ensureCsrfCookie() {
        if (readCookie(CSRF_COOKIE_NAME)) return;
        await fetch("/api/auth/me", { method: "GET", credentials: "include" });
    }

    async function request(url, options = {}) {
        const method = options.method ?? "GET";
        const headers = new Headers(options.headers ?? {});

        if (!["GET", "HEAD", "OPTIONS"].includes(method.toUpperCase())) {
            await ensureCsrfCookie();
            const csrfToken = readCookie(CSRF_COOKIE_NAME);
            if (csrfToken) headers.set(CSRF_HEADER_NAME, decodeURIComponent(csrfToken));
        }

        const response = await fetch(url, {
            ...options,
            method,
            headers,
            credentials: "include"
        });

        const isJson = response.headers.get("content-type")?.includes("application/json");
        const body = isJson ? await response.json() : null;
        return { response, body };
    }

    async function getCurrentUser() {
        const { response, body } = await request("/api/auth/me");
        return { status: response.status, body };
    }

    function login() {
        // Редирект на Spring Security OAuth2 login endpoint
        window.location.href = "/oauth2/authorization/keycloak";
    }

    async function logout() {
        // Логаут через Spring Security
        window.location.href = "/logout";
        return { response: { status: 302 }, body: null };
    }

    function extractDisplayName(userInfo = {}) {
        return userInfo.preferred_username ?? userInfo.email ?? userInfo.name ?? userInfo.sub ?? "Игрок";
    }

    function showAlert(targetId, type, message) {
        const container = document.getElementById(targetId);
        if (!container) return;
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert ${type} mb-2`;
        alertDiv.innerHTML = `<span>${message}</span>`;
        container.insertBefore(alertDiv, container.firstChild);
        setTimeout(() => alertDiv.remove(), 5000);
    }

    return {
        ensureCsrfCookie,
        getCurrentUser,
        login,
        logout,
        request,
        extractDisplayName,
        showAlert
    };
})();

window.AuthApi = AuthApi;