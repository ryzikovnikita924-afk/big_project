const AuthApi = (() => {

    async function request(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            credentials: "include",
            redirect: "manual"
        });

        const contentType = response.headers.get("content-type");

        let body = null;
        if (contentType && contentType.includes("application/json")) {
            try { body = await response.json(); } catch {}
        }

        return { response, body };
    }

    async function getCurrentUser() {
        try {
            const { response, body } = await request("/api/me");

            return {
                status: response.status,
                body
            };

        } catch (err) {
            console.error(err);
            return { status: 500, body: { authenticated: false } };
        }
    }

    function login(returnTo = "/index.html") {
        window.location.href = `/oauth2/start?rd=${encodeURIComponent(returnTo)}`;
    }

    function logout() {
        window.location.href = "/oauth2/sign_out";
    }

    function extractDisplayName(info = {}) {
        return info.email || info.preferred_username || info.user || "Игрок";
    }

    return { getCurrentUser, login, logout, extractDisplayName };
})();

window.AuthApi = AuthApi;