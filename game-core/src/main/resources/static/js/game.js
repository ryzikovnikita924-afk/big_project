// В самом начале файла game.js

const GameApi = (() => {
    const BASE_URL = window.location.port === '4180' ? '' : 'http://localhost:4180';

    async function request(endpoint, options = {}) {
        const url = `${BASE_URL}/api/game${endpoint}`;
        const response = await fetch(url, {
            ...options,
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                // Не авторизован - перенаправляем на логин
                window.location.href = '/login.html';
                throw new Error('Unauthorized');
            }
            throw new Error(`HTTP ${response.status}`);
        }

        const body = await response.json();
        return { response, body };
    }

    return {
        getMap: () => request('/map'),
        getGameState: () => request('/state'),
        startTurn: () => request('/turn/start', { method: 'POST' }),
        endTurn: () => request('/turn/end', { method: 'POST' }),
        attack: (x, y, troops) => request('/attack', {
            method: 'POST',
            body: JSON.stringify({ x, y, troops })
        }),
        getLeaderboard: () => request('/leaderboard'),
        getUserInfo: async () => {
            const response = await fetch('/api/auth/me', { credentials: 'include' });
            const data = await response.json();
            return data?.data || null;
        }
    };
})();

// Auth API для работы с аутентификацией
const AuthApi = (() => {
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    function readCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    }

    async function ensureCsrfCookie() {
        if (readCookie(CSRF_COOKIE_NAME)) return;
        await fetch("/api/auth/me", { credentials: "include" });
    }

    async function getCurrentUser() {
        try {
            await ensureCsrfCookie();
            const response = await fetch("/api/auth/me", { credentials: "include" });
            const data = await response.json();
            return data?.data || null;
        } catch (error) {
            console.error("Failed to get user:", error);
            return null;
        }
    }

    async function logout() {
        const response = await fetch("/api/auth/logout", {
            method: "POST",
            credentials: "include"
        });

        if (response.ok) {
            window.location.href = "/login.html?logout=true";
        }
    }

    function showAlert(message, type = "info") {
        // Создаем уведомление на странице
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} fixed top-4 right-4 z-50 shadow-lg max-w-md`;
        alertDiv.innerHTML = `
            <span>${message}</span>
            <button class="btn btn-sm btn-ghost" onclick="this.parentElement.remove()">✕</button>
        `;
        document.body.appendChild(alertDiv);

        setTimeout(() => alertDiv.remove(), 5000);
    }

    return {
        ensureCsrfCookie,
        getCurrentUser,
        logout,
        showAlert
    };
})();

// Инициализация игры
document.addEventListener('DOMContentLoaded', async () => {
    // Проверяем авторизацию
    const user = await AuthApi.getCurrentUser();
    if (!user?.authenticated) {
        window.location.href = '/login.html';
        return;
    }

    console.log('User authenticated:', user);

    // Обновляем UI с информацией о пользователе
    const userInfoElement = document.getElementById('user-info');
    if (userInfoElement && user.userInfo) {
        const displayName = user.userInfo.email || user.userInfo.name || user.userInfo.sub;
        userInfoElement.textContent = displayName;
    }

    // Инициализируем игровые компоненты
    await initGame();
});

async function initGame() {
    try {
        // Загружаем карту
        const { body: mapData } = await GameApi.getMap();
        console.log('Map loaded:', mapData);

        // Загружаем состояние игры
        const { body: gameState } = await GameApi.getGameState();
        console.log('Game state:', gameState);

        // Рендерим карту
        renderMap(mapData);
        renderUI(gameState);
    } catch (error) {
        console.error('Failed to initialize game:', error);
        AuthApi.showAlert('Ошибка загрузки игры', 'error');
    }
}

function renderMap(mapData) {
    // Ваша логика рендера карты
    const canvas = document.getElementById('game-canvas');
    if (!canvas) return;

    // Пример рендера
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#2d2d2d';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
}

function renderUI(gameState) {
    // Ваша логика рендера UI
    const scoreElement = document.getElementById('score');
    if (scoreElement && gameState.score) {
        scoreElement.textContent = gameState.score;
    }
}

// Экспортируем в global
window.GameApi = GameApi;
window.AuthApi = AuthApi;