package com.example.Controller;

import com.example.entity.PlayerEntity;
import com.example.service.GamePersistenceService;
import com.example.service.StatisticsService;
import com.example.world.GameWorld;
import com.example.service.TurnService;
import com.example.model.Player;
import com.example.model.Cell;
import com.example.model.ResourceType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
public class GameController {

    private final GameWorld gameWorld;
    private final TurnService turnService;
    private final StatisticsService statisticsService;
    private final GamePersistenceService gamePersistenceService;
    private String currentPlayerId;
    private List<Player> allPlayers;
    private String currentGameId;
    private Map<String, PlayerEntity> playerEntityMap = new HashMap<>();

    public GameController(GameWorld gameWorld,
                          TurnService turnService,
                          StatisticsService statisticsService,
                          GamePersistenceService gamePersistenceService) {
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        this.statisticsService = statisticsService;
        this.gamePersistenceService = gamePersistenceService;
        this.currentGameId = UUID.randomUUID().toString();
        initGame();
    }

    private void initGame() {
        if (!gameWorld.getCells().isEmpty()) {
            System.out.println("Мир уже инициализирован");
            return;
        }

        gameWorld.createWorld(10, 10);
        Player player1 = new Player("Красный Лорд");
        Player player2 = new Player("Синий Барон");
        gameWorld.addPlayer(player1, 2, 2);
        gameWorld.addPlayer(player2, 7, 7);

        allPlayers = new ArrayList<>();
        allPlayers.add(player1);
        allPlayers.add(player2);


        for (Player player : allPlayers) {
            PlayerEntity entity = statisticsService.getOrCreatePlayer(player.getId(), player.getName());
            playerEntityMap.put(player.getId(), entity);
        }

        turnService.initialize(allPlayers);

        Player firstPlayer = player1;
        currentPlayerId = firstPlayer.getId();

        gameWorld.start();
        System.out.println("✅ Игра инициализирована!");
        System.out.println("👑 Первый ход: " + firstPlayer.getName());
        System.out.println("✅ Автоматически выбран игрок: " + firstPlayer.getName());
        System.out.println("✅ currentPlayerId установлен: " + currentPlayerId);
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/api/players")
    @ResponseBody
    public List<Map<String, Object>> getPlayers() {
        List<Map<String, Object>> playersList = new ArrayList<>();
        Player currentTurnPlayer = turnService.getCurrentPlayer();

        for (Player player : gameWorld.getPlayers().values()) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("id", player.getId());
            playerData.put("name", player.getName());
            playerData.put("isCurrent", player.getId().equals(currentPlayerId));
            playerData.put("isTurn", currentTurnPlayer != null && player.getId().equals(currentTurnPlayer.getId()));
            playersList.add(playerData);
        }
        return playersList;
    }

    @PostMapping("/api/select-player")
    @ResponseBody
    public Map<String, Object> selectPlayer(@RequestBody Map<String, String> request) {
        String playerId = request.get("playerId");
        Player player = gameWorld.getPlayer(playerId);

        Map<String, Object> response = new HashMap<>();
        if (player != null) {
            currentPlayerId = playerId;
            response.put("success", true);
            response.put("message", "Выбран игрок: " + player.getName());
            response.put("playerId", playerId);
            response.put("playerName", player.getName());

            Player currentTurn = turnService.getCurrentPlayer();
            if (currentTurn != null && currentTurn.getId().equals(playerId)) {
                response.put("isYourTurn", true);
                response.put("message", "Выбран игрок: " + player.getName() + " (СЕЙЧАС ВАШ ХОД!)");
            } else {
                response.put("isYourTurn", false);
                response.put("currentTurnPlayer", currentTurn != null ? currentTurn.getName() : "unknown");
            }
        } else {
            response.put("success", false);
            response.put("message", "Игрок не найден");
        }
        return response;
    }

    @GetMapping("/api/map")
    @ResponseBody
    public Map<String, Object> getMap() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> cells = new ArrayList<>();

        for (Cell cell : gameWorld.getCells().values()) {
            Map<String, Object> cellData = new HashMap<>();
            cellData.put("x", cell.getX());
            cellData.put("y", cell.getY());
            cellData.put("owner", cell.getOwnerId());
            cellData.put("troops", cell.getTroopsCount());
            cellData.put("terrain", cell.getTerrain().name());
            cellData.put("level", cell.getLevel());
            cellData.put("isWater", cell.isWater());
            cells.add(cellData);
        }
        result.put("cells", cells);

        List<Map<String, Object>> playersList = new ArrayList<>();
        for (Player player : gameWorld.getPlayers().values()) {
            playersList.add(player.toMap());
        }
        result.put("players", playersList);
        result.put("currentPlayerId", currentPlayerId);
        result.put("currentPlayerName", getCurrentPlayerName());
        result.put("turnOwnerId", turnService.getCurrentPlayer() != null ? turnService.getCurrentPlayer().getId() : null);
        result.put("turnOwnerName", turnService.getCurrentPlayer() != null ? turnService.getCurrentPlayer().getName() : null);
        result.put("gameState", turnService.getState().toString());

        return result;
    }

    @PostMapping("/api/attack")
    @ResponseBody
    public Map<String, String> attack(@RequestBody AttackRequest request) {
        if (currentPlayerId == null) {
            return Map.of("status", "error", "message", "Игрок не выбран!");
        }

        Player turnOwner = turnService.getCurrentPlayer();
        if (turnOwner == null || !turnOwner.getId().equals(currentPlayerId)) {
            return Map.of("status", "error", "message",
                    "Сейчас не ваш ход! Сейчас ход игрока: " + (turnOwner != null ? turnOwner.getName() : "неизвестно"));
        }

        if (turnService.getState() != TurnService.GameState.PROCESSING) {
            return Map.of("status", "error", "message", "Сначала начните ход кнопкой 'Начать ход'!");
        }

        List<Cell> playerCells = new ArrayList<>();
        for (Cell cell : gameWorld.getCells().values()) {
            if (currentPlayerId.equals(cell.getOwnerId()) && !cell.isWater()) {
                playerCells.add(cell);
            }
        }

        if (playerCells.isEmpty()) {
            return Map.of("status", "error", "message", "У вас нет клеток для атаки!");
        }

        Cell fromCell = playerCells.get(0);
        String toId = request.toX + ":" + request.toY;
        Cell toCell = gameWorld.getCell(toId);

        if (toCell == null) {
            return Map.of("status", "error", "message", "Клетка не найдена!");
        }
        if (request.troops <= 0 || request.troops > fromCell.getTroopsCount()) {
            return Map.of("status", "error", "message", "Недостаточно войск! У вас " + fromCell.getTroopsCount());
        }

        try {
            gameWorld.executeInstantAttack(fromCell.getId(), toId, request.troops, currentPlayerId);
            // Автосохранение после атаки
            gamePersistenceService.autoSave();
            return Map.of("status", "ok", "message", "⚔️ Атака выполнена!");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @PostMapping("/api/start-turn")
    @ResponseBody
    public Map<String, Object> startTurn() {
        Map<String, Object> response = new HashMap<>();

        if (currentPlayerId == null) {
            response.put("success", false);
            response.put("message", "Ошибка: игрок не выбран");
            return response;
        }

        Player turnOwner = turnService.getCurrentPlayer();
        if (turnOwner == null) {
            response.put("success", false);
            response.put("message", "Ошибка: нет текущего игрока");
            return response;
        }

        if (!turnOwner.getId().equals(currentPlayerId)) {
            response.put("success", false);
            response.put("message", "Сейчас не ваш ход! Сейчас ход игрока: " + turnOwner.getName());
            response.put("currentTurnPlayer", turnOwner.getName());
            return response;
        }

        if (turnService.getState() != TurnService.GameState.WAITING) {
            response.put("success", false);
            response.put("message", "Ход уже начат! Текущий статус: " + turnService.getState());
            return response;
        }

        boolean success = turnService.startTurn(currentPlayerId);
        response.put("success", success);

        if (success) {
            Player current = turnService.getCurrentPlayer();
            response.put("message", "🎯 Ваш ход начался! Ресурсы получены.");
            response.put("gameState", turnService.getState().toString());
            if (current != null) {
                response.put("currentPlayer", current.getName());
                response.put("turn", turnService.getTurnNumber());
                response.put("gold", current.getResource(ResourceType.GOLD));
                response.put("wood", current.getResource(ResourceType.WOOD));
                response.put("food", current.getResource(ResourceType.FOOD));
                response.put("troops", current.getTotalTroops());
            }
            // Автосохранение после начала хода
            gamePersistenceService.autoSave();
        } else {
            response.put("message", "⏳ Не удалось начать ход. Попробуйте еще раз.");
        }
        return response;
    }

    @PostMapping("/api/end-turn")
    @ResponseBody
    public Map<String, Object> endTurn() {
        Map<String, Object> response = new HashMap<>();

        if (currentPlayerId == null) {
            response.put("success", false);
            response.put("message", "Ошибка: игрок не выбран");
            return response;
        }

        if (turnService.getState() != TurnService.GameState.PROCESSING) {
            response.put("success", false);
            response.put("message", "Ход не был начат! Текущий статус: " + turnService.getState());
            return response;
        }

        boolean success = turnService.endTurn(currentPlayerId);
        System.out.println("End turn success: " + success);

        if (success) {
            response.put("success", true);
            response.put("message", "✅ Ход завершен!");

            Player nextPlayer = turnService.getCurrentPlayer();
            response.put("nextPlayer", nextPlayer != null ? nextPlayer.getName() : null);
            response.put("nextPlayerId", nextPlayer != null ? nextPlayer.getId() : null);
            response.put("gameState", turnService.getState().toString());

            // Автосохранение после завершения хода
            gamePersistenceService.autoSave();
        } else {
            response.put("success", false);
            response.put("message", "❌ Не удалось завершить ход");
        }

        return response;
    }

    @GetMapping("/api/game-state")
    @ResponseBody
    public Map<String, Object> getGameState() {
        Map<String, Object> state = new HashMap<>();
        Player turnOwner = turnService.getCurrentPlayer();
        Player winner = gameWorld.getWinner();
        Player myPlayer = gameWorld.getPlayer(currentPlayerId);

        state.put("currentTurnPlayer", turnOwner != null ? turnOwner.getName() : null);
        state.put("currentTurnPlayerId", turnOwner != null ? turnOwner.getId() : null);
        state.put("turn", turnService.getTurnNumber());
        state.put("gameState", turnService.getState().toString());
        state.put("winner", winner != null ? winner.getName() : null);
        state.put("myPlayerId", currentPlayerId);
        state.put("myPlayerName", myPlayer != null ? myPlayer.getName() : null);

        boolean canStartTurn = currentPlayerId != null &&
                turnOwner != null &&
                currentPlayerId.equals(turnOwner.getId()) &&
                turnService.getState() == TurnService.GameState.WAITING;

        boolean canAttack = currentPlayerId != null &&
                turnOwner != null &&
                currentPlayerId.equals(turnOwner.getId()) &&
                turnService.getState() == TurnService.GameState.PROCESSING;

        state.put("canStartTurn", canStartTurn);
        state.put("canAttack", canAttack);
        state.put("isMyTurn", canStartTurn || canAttack);

        if (myPlayer != null) {
            state.put("myGold", myPlayer.getResource(ResourceType.GOLD));
            state.put("myWood", myPlayer.getResource(ResourceType.WOOD));
            state.put("myFood", myPlayer.getResource(ResourceType.FOOD));
            state.put("myTroops", myPlayer.getTotalTroops());
            state.put("myCells", myPlayer.getCapturedCellIds().size());
        }

        System.out.println("Game state: turnOwner=" + (turnOwner != null ? turnOwner.getName() : "null") +
                ", myPlayer=" + (myPlayer != null ? myPlayer.getName() : "null") +
                ", gameState=" + turnService.getState() +
                ", canStartTurn=" + canStartTurn +
                ", canAttack=" + canAttack);

        return state;
    }

    @PostMapping("/api/end-game")
    @ResponseBody
    public Map<String, Object> endGame() {
        Player winner = gameWorld.getWinner();

        // Сохраняем статистику для всех игроков
        for (Player player : gameWorld.getPlayers().values()) {
            PlayerEntity playerEntity = playerEntityMap.get(player.getId());
            if (playerEntity == null) continue;

            int cellsCaptured = player.getCapturedCellIds().size();
            int troopsKilled = calculateTroopsKilled(player);
            boolean isWinner = winner != null && winner.getId().equals(player.getId());
            int turnsPlayed = turnService.getTurnNumber();
            int score = cellsCaptured * 10 + troopsKilled + (isWinner ? 100 : 0);

            // Обновляем статистику игрока
            statisticsService.updatePlayerStats(playerEntity, cellsCaptured, troopsKilled, score, isWinner);
            statisticsService.addGameHistory(
                    currentGameId,
                    playerEntity,
                    getPlayerColor(player),
                    isWinner,
                    cellsCaptured,
                    troopsKilled,
                    turnsPlayed,
                    getGameDuration()
            );
        }


        gamePersistenceService.finishGame(winner);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("winner", winner != null ? winner.getName() : null);
        return response;
    }

    @GetMapping("/api/debug/status")
    @ResponseBody
    public Map<String, Object> debugStatus() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("turnServiceState", turnService.getState().toString());
        debug.put("turnServiceCurrentPlayer", turnService.getCurrentPlayer() != null ? turnService.getCurrentPlayer().getName() : "null");
        debug.put("turnServiceCurrentPlayerId", turnService.getCurrentPlayer() != null ? turnService.getCurrentPlayer().getId() : "null");
        debug.put("controllerCurrentPlayerId", currentPlayerId);
        debug.put("controllerCurrentPlayerName", getCurrentPlayerName());
        return debug;
    }

    private String getCurrentPlayerName() {
        Player player = gameWorld.getPlayer(currentPlayerId);
        return player != null ? player.getName() : "Не выбран";
    }

    private int calculateTroopsKilled(Player player) {
        // TODO: реализовать подсчет убитых войск
        return 0;
    }

    private String getPlayerColor(Player player) {
        if (player.getName().equals("Красный Лорд")) {
            return "RED";
        } else if (player.getName().equals("Синий Барон")) {
            return "BLUE";
        }
        return "NEUTRAL";
    }

    private int getGameDuration() {
        // TODO: рассчитать длительность игры в минутах
        return 0;
    }

    static class AttackRequest {
        public int toX, toY, troops;
    }
}