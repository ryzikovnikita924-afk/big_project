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
        if (readCookie(CSRF_COOKIE_NAME)) {
            return;
        }
        await fetch("/api/auth/me", {
            method: "GET",
            credentials: "include"
        });
    }

    async function request(url, options = {}) {
        const method = options.method ?? "GET";
        const headers = new Headers(options.headers ?? {});

        if (!["GET", "HEAD", "OPTIONS"].includes(method.toUpperCase())) {
            await ensureCsrfCookie();
            const csrfToken = readCookie(CSRF_COOKIE_NAME);
            if (csrfToken) {
                headers.set(CSRF_HEADER_NAME, decodeURIComponent(csrfToken));
            }
        }

        const response = await fetch(url, {
            ...options,
            method,
            headers,
            credentials: "include"
        });

        const isJson = response.headers.get("content-type")?.includes("application/json");
        const body = isJson ? await response.json() : null;
        return {response, body};
    }

    async function getCurrentUser() {
        const {response, body} = await request("/api/auth/me");
        return {
            status: response.status,
            body
        };
    }

    async function login() {
        // Перенаправление на OAuth2 Proxy
        window.location.assign("/oauth2/authorization/dex");
    }

    async function logout() {
        // Логаут через backend
        const result = await request("/api/auth/logout", {
            method: "POST"
        });

        // Дополнительно очищаем сессию OAuth2 Proxy
        if (result.response.ok) {
            // Если есть прямой доступ к OAuth2 Proxy logout
            await fetch("/oauth2/sign_out", { credentials: "include" }).catch(() => {});
        }

        return result;
    }

    function extractDisplayName(userInfo = {}) {
        return userInfo.email ?? userInfo.name ?? userInfo.preferred_username ?? userInfo.sub ?? "unknown";
    }

    function showAlert(targetId, type, message) {
        const container = document.getElementById(targetId);
        if (!container) {
            return;
        }

        container.innerHTML = `
            <div class="alert ${type} shadow-lg">
                <span>${message}</span>
            </div>
        `;

        // Автоматическое скрытие через 5 секунд
        setTimeout(() => {
            if (container.innerHTML === message) {
                container.innerHTML = "";
            }
        }, 5000);
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