const GameApi = (() => {
    async function request(endpoint, options = {}) {
        const response = await fetch(`/api/game${endpoint}`, {
            ...options,
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                AuthApi.login();
                throw new Error('Unauthorized');
            }
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
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
        getLeaderboard: () => request('/leaderboard')
    };
})();

window.GameApi = GameApi;