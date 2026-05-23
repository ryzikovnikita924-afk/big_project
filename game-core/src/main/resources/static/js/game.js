// game.js - Вынесенная игровая логика

class GeoStratGame {
    constructor() {
        this.myPlayerId = null;
        this.myPlayerName = null;
        this.gameActive = false;
        this.selectedCell = null;
        this.mapData = null;
        this.buildingsData = null;
        this.buildMode = false;
        this.gameState = {
            canStartTurn: false,
            canAttack: false,
            canEndTurn: false,
            hasCapturedThisTurn: false,
            gameState: 'WAITING',
            currentTurnPlayer: null,
            turn: 1
        };
        this.GameApi = this.initGameApi();
    }

    initGameApi() {
        return {
            request: async (endpoint, options = {}) => {
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
                        throw new Error('Unauthorized');
                    }
                    throw new Error(`HTTP ${response.status}`);
                }

                return await response.json();
            },

            startVsAI: (playerId, playerName, difficulty) => this.GameApi.request('/start-vs-ai', {
                method: 'POST',
                body: JSON.stringify({ playerId, playerName, difficulty })
            }),

            reset: () => this.GameApi.request('/reset', { method: 'POST' }),
            getMap: () => this.GameApi.request('/map'),
            getGameState: () => this.GameApi.request('/state'),
            startTurn: () => this.GameApi.request('/turn/start', { method: 'POST' }),
            endTurn: () => this.GameApi.request('/turn/end', { method: 'POST' }),
            attack: (x, y) => this.GameApi.request('/attack', {
                method: 'POST',
                body: JSON.stringify({ x, y, troops: 1 })
            }),
            build: (x, y, buildingType) => this.GameApi.request('/build', {
                method: 'POST',
                body: JSON.stringify({ x, y, buildingType })
            }),
            getLeaderboard: () => this.GameApi.request('/leaderboard'),
            getBuildings: () => this.GameApi.request('/buildings')
        };
    }

    async loadMap() {
        try {
            const data = await this.GameApi.getMap();
            if (data) {
                this.mapData = data;
                this.renderMap();
            }
        } catch (error) {
            console.error('Load map error:', error);
        }
    }

    async loadBuildings() {
        try {
            const data = await this.GameApi.getBuildings();
            if (data && Array.isArray(data)) {
                this.buildingsData = data;
                this.renderBuildPanel();
            }
        } catch (error) {
            console.error('Load buildings error:', error);
        }
    }

    getBuildingIcon(type) {
        const icons = {
            'BARRACKS': '🏪',
            'PORT': '⚓',
            'SAWMILL': '🪚',
            'FARM': '🚜'
        };
        return icons[type] || '🏗️';
    }

    getBuildingName(type) {
        const names = {
            'BARRACKS': 'Казармы (+5 войск/ход)',
            'PORT': 'Порт (+30 золота/ход)',
            'SAWMILL': 'Лесопилка (+20 дерева/ход)',
            'FARM': 'Ферма (+15 еды/ход)'
        };
        return names[type] || type;
    }

    canBuildOnTerrain(buildingType, terrain) {
        const allowed = {
            'BARRACKS': 'PLAIN',
            'PORT': 'WATER',
            'SAWMILL': 'FOREST',
            'FARM': 'PLAIN'
        };
        return allowed[buildingType] === terrain;
    }

    renderBuildPanel() {
        const panel = document.getElementById('available-buildings');
        if (!panel) return;

        if (!this.selectedCell) {
            panel.innerHTML = '<div class="text-center text-gray-500 py-2">Выберите клетку для строительства</div>';
            return;
        }

        if (!this.buildingsData) {
            panel.innerHTML = '<div class="text-center text-gray-500 py-2">Загрузка...</div>';
            return;
        }

        panel.innerHTML = '';
        const cell = this.selectedCell;

        for (const building of this.buildingsData) {
            const canBuild = this.canBuildOnTerrain(building.type, cell.terrain);
            const btn = document.createElement('button');
            btn.className = `btn btn-sm w-full mb-2 ${canBuild ? 'btn-primary' : 'btn-disabled opacity-50'}`;
            btn.innerHTML = `${this.getBuildingIcon(building.type)} ${building.name} | 🪙${building.costGold} 🌲${building.costWood}`;

            if (canBuild) {
                btn.onclick = () => this.buildOnSelectedCell(building.type);
                btn.title = `Построить ${building.name}`;
            } else {
                btn.disabled = true;
                btn.title = `Нельзя построить на ${cell.terrain}`;
            }
            panel.appendChild(btn);
        }
    }

    async buildOnSelectedCell(buildingType) {
        if (!this.selectedCell) {
            window.AuthApi.showAlert('build-alert', 'alert-warning', 'Сначала выберите клетку!');
            return;
        }

        const body = await this.GameApi.build(this.selectedCell.x, this.selectedCell.y, buildingType);

        if (body?.success) {
            this.addBattleLog(`🏗️ Построено: ${body.buildingName} на клетке [${this.selectedCell.x},${this.selectedCell.y}]!`);
            this.selectedCell = null;
            this.buildMode = false;
            document.getElementById('build-panel')?.classList.add('hidden');
            document.getElementById('build-mode-btn')?.classList.remove('btn-active');
            await this.loadMap();
            await this.checkGameState();
        } else {
            window.AuthApi.showAlert('build-alert', 'alert-error', body?.message || 'Ошибка строительства');
        }
    }

    toggleBuildMode() {
        if (!this.gameState.canStartTurn && !this.gameState.canEndTurn) {
            window.AuthApi.showAlert('turn-alert', 'alert-warning', 'Сейчас не ваш ход!');
            return;
        }

        this.buildMode = !this.buildMode;
        const btn = document.getElementById('build-mode-btn');
        if (this.buildMode) {
            btn?.classList.add('btn-active');
            window.AuthApi.showAlert('build-alert', 'alert-info', 'Режим строительства включен. Нажмите на свою клетку для строительства.');
        } else {
            btn?.classList.remove('btn-active');
            this.selectedCell = null;
            document.getElementById('build-panel')?.classList.add('hidden');
            window.AuthApi.showAlert('build-alert', 'alert-info', 'Режим строительства выключен.');
        }
        this.renderMap();
    }

    renderMap() {
        const mapDiv = document.getElementById('map');
        if (!this.mapData || !mapDiv) return;

        mapDiv.innerHTML = '';
        for (let y = 0; y < 10; y++) {
            for (let x = 0; x < 10; x++) {
                const cell = this.mapData.cells?.find(c => c.x === x && c.y === y);
                if (!cell) continue;

                const cellDiv = document.createElement('div');
                cellDiv.className = 'cell';

                // Можно ли строить на этой клетке
                const canBuildHere = cell.owner === this.myPlayerId && !cell.building && !cell.isWater;

                if (this.buildMode && canBuildHere) {
                    cellDiv.classList.add('can-build');
                }

                if (this.gameState.canAttack && !this.buildMode && cell.owner !== this.myPlayerId && !cell.isWater) {
                    cellDiv.classList.add('can-attack');
                } else if (!this.gameState.canAttack) {
                    cellDiv.classList.add('cannot-attack');
                }

                if (cell.isWater) cellDiv.classList.add('water');
                else if (cell.owner === null) cellDiv.classList.add('neutral');
                else if (cell.owner === this.myPlayerId) cellDiv.classList.add('player1');
                else cellDiv.classList.add('player2');

                let terrainIcon = '⬜';
                switch(cell.terrain) {
                    case 'FOREST': terrainIcon = '🌲'; break;
                    case 'MOUNTAIN': terrainIcon = '⛰️'; break;
                    case 'CITY': terrainIcon = '🏙️'; break;
                    case 'WATER': terrainIcon = '💧'; break;
                    default: terrainIcon = '🌾';
                }

                let buildingHtml = '';
                if (cell.building && cell.building !== 'NONE') {
                    buildingHtml = `<div class="building-icon" title="${this.getBuildingName(cell.building)}">${this.getBuildingIcon(cell.building)}</div>`;
                }

                cellDiv.innerHTML = `<div class="terrain-icon">${terrainIcon}</div>${buildingHtml}`;

                cellDiv.onclick = () => {
                    if (this.buildMode && canBuildHere) {
                        this.selectCellForBuild(cell);
                    } else if (this.gameState.canAttack && !this.buildMode && cell.owner !== this.myPlayerId && !cell.isWater) {
                        this.handleCellClick(cell);
                    } else if (cell.owner === this.myPlayerId && !this.buildMode) {
                        window.AuthApi.showAlert('turn-alert', 'alert-info', 'Включите режим строительства (🏗️) для постройки зданий');
                    }
                };

                if (this.selectedCell && this.selectedCell.x === x && this.selectedCell.y === y) {
                    cellDiv.classList.add('selected');
                }
                mapDiv.appendChild(cellDiv);
            }
        }
    }

    selectCellForBuild(cell) {
        this.selectedCell = cell;
        this.renderMap();
        this.renderBuildPanel();
        document.getElementById('build-panel')?.classList.remove('hidden');
        window.AuthApi.showAlert('build-alert', 'alert-info', `Выбрана клетка [${cell.x},${cell.y}]. Выберите здание для строительства.`);
    }

    async handleCellClick(cell) {
        if (!this.gameActive) return;

        if (cell.owner === this.myPlayerId || cell.isWater) {
            window.AuthApi.showAlert('turn-alert', 'alert-warning', 'Нельзя атаковать свою клетку или воду!');
            return;
        }

        if (!this.gameState.canAttack) {
            if (this.gameState.hasCapturedThisTurn) {
                window.AuthApi.showAlert('turn-alert', 'alert-warning', 'Вы уже захватили клетку в этом ходу! Завершите ход.');
            } else if (this.gameState.canStartTurn) {
                window.AuthApi.showAlert('turn-alert', 'alert-warning', 'Сначала начните ход кнопкой "Начать ход"!');
            } else {
                window.AuthApi.showAlert('turn-alert', 'alert-warning', 'Сейчас не ваш ход!');
            }
            return;
        }

        const body = await this.GameApi.attack(cell.x, cell.y);

        if (body?.status === 'ok') {
            this.addBattleLog(`⚔️ Захвачена клетка [${cell.x},${cell.y}]! Осталось войск: ${body.troops}`);
            this.selectedCell = null;
            document.getElementById('build-panel')?.classList.add('hidden');
            await this.loadMap();
            await this.checkGameState();
        } else {
            window.AuthApi.showAlert('turn-alert', 'alert-error', body?.message || 'Ошибка атаки');
        }
    }

    async checkGameState() {
        try {
            const body = await this.GameApi.getGameState();
            if (body) {
                this.gameState = body;

                document.getElementById('turn-number').innerText = body.turn || 1;
                document.getElementById('current-player').innerText = body.currentTurnPlayer || '-';

                const startTurnBtn = document.getElementById('start-turn-btn');
                const endTurnBtn = document.getElementById('end-turn-btn');
                const turnStatus = document.getElementById('turn-status');

                if (body.winner) {
                    turnStatus.innerHTML = `🏆 ПОБЕДИТЕЛЬ: ${body.winner}! 🏆`;
                    startTurnBtn?.classList.add('hidden');
                    endTurnBtn?.classList.add('hidden');
                    document.getElementById('build-panel')?.classList.add('hidden');
                    this.selectedCell = null;
                    return;
                }

                if (body.canEndTurn) {
                    startTurnBtn?.classList.add('hidden');
                    endTurnBtn?.classList.remove('hidden');
                    turnStatus.innerHTML = body.hasCapturedThisTurn
                        ? `🎲 ВАШ ХОД! Клетка захвачена. Нажмите "Завершить ход"`
                        : `🎲 ВАШ ХОД! Атакуйте врага или включите строительство (🏗️)`;
                } else if (body.canStartTurn) {
                    startTurnBtn?.classList.remove('hidden');
                    endTurnBtn?.classList.add('hidden');
                    turnStatus.innerHTML = `🎯 ВАШ ХОД! Нажмите "Начать ход"`;
                    document.getElementById('build-panel')?.classList.add('hidden');
                    this.selectedCell = null;
                    this.buildMode = false;
                    document.getElementById('build-mode-btn')?.classList.remove('btn-active');
                } else {
                    startTurnBtn?.classList.add('hidden');
                    endTurnBtn?.classList.add('hidden');
                    turnStatus.innerHTML = `⏳ Ход игрока: ${body.currentTurnPlayer || 'противника'}`;
                    document.getElementById('build-panel')?.classList.add('hidden');
                    this.selectedCell = null;
                    this.buildMode = false;
                    document.getElementById('build-mode-btn')?.classList.remove('btn-active');
                }

                this.updateResources(body);
                this.renderMap();
            }
        } catch (error) {
            console.error('Check game state error:', error);
        }
    }

    updateResources(state) {
        const elements = {
            gold: 'myGold',
            wood: 'myWood',
            food: 'myFood',
            'total-troops': 'myTroops'
        };
        for (const [id, key] of Object.entries(elements)) {
            const el = document.getElementById(id);
            if (el) el.innerText = state[key] || 0;
        }

        const cellsElement = document.getElementById('my-cells');
        if (cellsElement && state.myCells !== undefined) {
            cellsElement.innerText = state.myCells;
        }
    }

    async loadLeaderboard() {
        try {
            const data = await this.GameApi.getLeaderboard();
            if (data && Array.isArray(data)) {
                const container = document.getElementById('leaderboard-list');
                if (container) {
                    container.innerHTML = data.map((player, idx) => `
                        <div class="flex justify-between items-center">
                            <span class="${idx < 3 ? 'font-bold text-primary' : ''}">${idx + 1}. ${player.name}</span>
                            <span>🏆 ${player.totalWins || 0} побед</span>
                        </div>
                    `).join('');
                }
            }
        } catch (error) {
            console.error('Load leaderboard error:', error);
        }
    }

    addBattleLog(message) {
        const logDiv = document.getElementById('battle-log');
        if (!logDiv) return;
        const entry = document.createElement('div');
        entry.innerHTML = `[${new Date().toLocaleTimeString()}] ${message}`;
        logDiv.insertBefore(entry, logDiv.firstChild);
        while (logDiv.children.length > 50) logDiv.removeChild(logDiv.lastChild);
    }

    setupEventListeners() {
        document.getElementById('start-turn-btn')?.addEventListener('click', async () => {
            const body = await this.GameApi.startTurn();
            if (body?.success) {
                this.addBattleLog('🎮 Начат новый ход! Получены ресурсы.');
                this.selectedCell = null;
                this.buildMode = false;
                document.getElementById('build-panel')?.classList.add('hidden');
                document.getElementById('build-mode-btn')?.classList.remove('btn-active');
                await this.checkGameState();
                await this.loadMap();
            } else {
                window.AuthApi.showAlert('turn-alert', 'alert-error', body?.message || 'Не удалось начать ход');
            }
        });

        document.getElementById('end-turn-btn')?.addEventListener('click', async () => {
            const body = await this.GameApi.endTurn();
            if (body?.success) {
                this.addBattleLog('✅ Ход завершен!');
                this.selectedCell = null;
                this.buildMode = false;
                document.getElementById('build-panel')?.classList.add('hidden');
                document.getElementById('build-mode-btn')?.classList.remove('btn-active');
                await this.checkGameState();
                await this.loadMap();
            } else {
                window.AuthApi.showAlert('turn-alert', 'alert-error', body?.message || 'Не удалось завершить ход');
            }
        });

        document.getElementById('build-mode-btn')?.addEventListener('click', () => {
            this.toggleBuildMode();
        });
    }

    renderEmptyMap() {
        const mapDiv = document.getElementById('map');
        if (!mapDiv) return;

        mapDiv.innerHTML = '';
        for (let y = 0; y < 10; y++) {
            for (let x = 0; x < 10; x++) {
                const cellDiv = document.createElement('div');
                cellDiv.className = 'cell neutral';
                cellDiv.style.opacity = '0.5';
                cellDiv.innerHTML = '<div class="terrain-icon">❓</div>';
                mapDiv.appendChild(cellDiv);
            }
        }
    }

    async startNewGameVsAI(difficulty) {
        if (!this.myPlayerId) {
            window.AuthApi.showAlert('turn-alert', 'alert-error', 'ID игрока не найден. Попробуйте выйти и войти снова.');
            return false;
        }

        const result = await this.GameApi.startVsAI(this.myPlayerId, this.myPlayerName, difficulty);

        if (result.success) {
            this.gameActive = true;
            this.buildMode = false;
            window.AuthApi.showAlert('turn-alert', 'alert-success', result.message);
            await this.loadMap();
            await this.checkGameState();
            await this.loadBuildings();
            return true;
        } else {
            window.AuthApi.showAlert('turn-alert', 'alert-error', result.message);
            return false;
        }
    }

    async resetGame() {
        await this.GameApi.reset();
        this.selectedCell = null;
        this.buildMode = false;
        this.gameActive = true;
        document.getElementById('build-panel')?.classList.add('hidden');
        document.getElementById('build-mode-btn')?.classList.remove('btn-active');
        await this.loadMap();
        await this.checkGameState();
    }
}

// Экспортируем класс для использования
if (typeof module !== 'undefined' && module.exports) {
    module.exports = GeoStratGame;
}