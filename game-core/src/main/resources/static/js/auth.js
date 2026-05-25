const AuthApi = (() => {

    let cachedUserId = null;
    let cachedUserInfo = null;

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

            if (response.status === 200 && body?.authenticated) {
                cachedUserInfo = body.userInfo;
                cachedUserId = body.userInfo?.preferred_username || body.userInfo?.email || body.userInfo?.user;
            }

            return {
                status: response.status,
                body
            };

        } catch (err) {
            console.error(err);
            return { status: 500, body: { authenticated: false } };
        }
    }

    function getUserId() {
        return cachedUserId;
    }

    function getUserInfo() {
        return cachedUserInfo;
    }

    function login(returnTo = "/index.html") {
        window.location.href = `/oauth2/start?rd=${encodeURIComponent(returnTo)}`;
    }

    function logout() {
        window.location.href = "/oauth2/sign_out";
    }

    function extractDisplayName(info = {}) {
        return info.preferred_username || info.email || info.user || "Игрок";
    }

    function showAlert(elementId, type, message) {
        const alertDiv = document.getElementById(elementId);
        if (!alertDiv) return;
        alertDiv.innerHTML = `<div class="alert ${type} shadow-lg"><span>${message}</span></div>`;
        setTimeout(() => {
            if (alertDiv.firstChild) alertDiv.removeChild(alertDiv.firstChild);
        }, 5000);
    }

    return {
        getCurrentUser,
        getUserId,
        getUserInfo,
        login,
        logout,
        extractDisplayName,
        showAlert
    };
})();

window.AuthApi = AuthApi;