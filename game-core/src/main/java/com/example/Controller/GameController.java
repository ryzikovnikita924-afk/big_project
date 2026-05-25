package com.example.Controller;

import com.example.entity.PlayerEntity;
import com.example.model.*;
import com.example.service.GamePersistenceService;
import com.example.service.StatisticsService;
import com.example.service.TurnService;
import com.example.world.GameWorld;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameWorld gameWorld;
    private final TurnService turnService;
    private final GamePersistenceService gamePersistenceService;
    private final StatisticsService statisticsService;

    private final Map<String, String> userCurrentPlayerIds = new ConcurrentHashMap<>();
    private final Map<String, Boolean> userStatisticsUpdated = new ConcurrentHashMap<>();

    public GameController(GameWorld gameWorld,
                          TurnService turnService,
                          GamePersistenceService gamePersistenceService,
                          StatisticsService statisticsService) {
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        this.gamePersistenceService = gamePersistenceService;
        this.statisticsService = statisticsService;
    }

    private String getCurrentUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            System.out.println("📌 Получен userId из заголовка: " + userId);
            return userId;
        }

        userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            userId = "anonymous_" + System.currentTimeMillis();
            request.getSession().setAttribute("userId", userId);
        }
        System.out.println("📌 Получен userId из сессии: " + userId);
        return userId;
    }

    private String getCurrentPlayerId(String userId) {
        return userCurrentPlayerIds.get(userId);
    }

    private boolean isStatisticsUpdated(String userId) {
        return userStatisticsUpdated.getOrDefault(userId, false);
    }

    private void setStatisticsUpdated(String userId, boolean updated) {
        userStatisticsUpdated.put(userId, updated);
    }

    @GetMapping("/map")
    public Map<String, Object> getMap(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        String currentPlayerId = getCurrentPlayerId(userId);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> cells = new ArrayList<>();

        for (Cell cell : gameWorld.getCells().values()) {
            Map<String, Object> cellData = new HashMap<>();
            cellData.put("x", cell.getX());
            cellData.put("y", cell.getY());
            cellData.put("owner", cell.getOwnerId());
            cellData.put("terrain", cell.getTerrain().name());
            cellData.put("level", cell.getLevel());
            cellData.put("isWater", cell.isWater());
            cellData.put("building", cell.hasBuilding() ? cell.getBuilding().toString() : null);
            cellData.put("buildingName", cell.hasBuilding() ? cell.getBuilding().getDisplayName() : null);
            cells.add(cellData);
        }

        result.put("cells", cells);
        result.put("totalCells", cells.size());

        List<Map<String, Object>> playersList = new ArrayList<>();
        for (Player player : gameWorld.getPlayers().values()) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("id", player.getId());
            playerData.put("name", player.getName());
            playerData.put("totalTroops", player.getTotalTroops());
            playerData.put("cellsCount", player.getCapturedCells().size());

            PlayerEntity dbPlayer = statisticsService.getPlayerById(player.getId());
            playerData.put("totalWins", dbPlayer != null ? dbPlayer.getTotalWins() : 0);
            playerData.put("totalGames", dbPlayer != null ? dbPlayer.getTotalGames() : 0);
            playersList.add(playerData);
        }
        result.put("players", playersList);

        return result;
    }

    @GetMapping("/state")
    public Map<String, Object> getGameState(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        String currentPlayerId = getCurrentPlayerId(userId);

        Map<String, Object> state = new HashMap<>();

        if (turnService.isGameFinished()) {
            Player winner = turnService.getWinner();
            state.put("gameState", "FINISHED");
            state.put("winner", winner != null ? winner.getName() : "Ничья");
            state.put("canStartTurn", false);
            state.put("canAttack", false);
            state.put("canEndTurn", false);
            state.put("isMyTurn", false);
            state.put("myGold", 0);
            state.put("myWood", 0);
            state.put("myFood", 0);
            state.put("myTroops", 0);
            state.put("myCells", 0);
            state.put("turn", turnService.getTurnNumber());
            state.put("hasCapturedThisTurn", false);

            if (!isStatisticsUpdated(userId) && winner != null) {
                updateWinnerStatistics(winner);
                for (Player player : gameWorld.getPlayers().values()) {
                    if (!player.getId().equals(winner.getId())) {
                        updateLoserStatistics(player);
                    }
                }
                setStatisticsUpdated(userId, true);
                gamePersistenceService.clearSave(userId);
            }
            return state;
        }

        Player turnOwner = turnService.getCurrentPlayer();
        Player winner = gameWorld.getWinner();
        Player myPlayer = currentPlayerId != null ? gameWorld.getPlayer(currentPlayerId) : null;

        state.put("currentTurnPlayer", turnOwner != null ? turnOwner.getName() : null);
        state.put("currentTurnPlayerId", turnOwner != null ? turnOwner.getId() : null);
        state.put("turn", turnService.getTurnNumber());
        state.put("gameState", turnService.getState().toString());
        state.put("winner", winner != null ? winner.getName() : null);
        state.put("hasCapturedThisTurn", turnService.hasCapturedThisTurn());

        boolean canStartTurn = currentPlayerId != null &&
                turnOwner != null &&
                currentPlayerId.equals(turnOwner.getId()) &&
                turnService.getState() == TurnService.GameState.WAITING;

        boolean canAttack = currentPlayerId != null &&
                turnOwner != null &&
                currentPlayerId.equals(turnOwner.getId()) &&
                turnService.getState() == TurnService.GameState.PROCESSING &&
                !turnService.hasCapturedThisTurn();

        boolean canEndTurn = currentPlayerId != null &&
                turnOwner != null &&
                currentPlayerId.equals(turnOwner.getId()) &&
                turnService.getState() == TurnService.GameState.PROCESSING;

        state.put("canStartTurn", canStartTurn);
        state.put("canAttack", canAttack);
        state.put("canEndTurn", canEndTurn);
        state.put("isMyTurn", canStartTurn || canEndTurn);

        if (myPlayer != null) {
            state.put("myGold", myPlayer.getResource(ResourceType.GOLD));
            state.put("myWood", myPlayer.getResource(ResourceType.WOOD));
            state.put("myFood", myPlayer.getResource(ResourceType.FOOD));
            state.put("myTroops", myPlayer.getTotalTroops());
            state.put("myCells", myPlayer.getCapturedCells().size());

            PlayerEntity dbPlayer = statisticsService.getPlayerById(myPlayer.getId());
            state.put("myTotalWins", dbPlayer != null ? dbPlayer.getTotalWins() : 0);
            state.put("myTotalGames", dbPlayer != null ? dbPlayer.getTotalGames() : 0);
        } else {
            state.put("myGold", 0);
            state.put("myWood", 0);
            state.put("myFood", 0);
            state.put("myTroops", 0);
            state.put("myCells", 0);
            state.put("myTotalWins", 0);
            state.put("myTotalGames", 0);
        }

        return state;
    }

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        List<PlayerEntity> topPlayers = statisticsService.getLeaderboard();
        List<Map<String, Object>> leaderboard = new ArrayList<>();

        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerEntity player = topPlayers.get(i);
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", player.getName());
            entry.put("totalWins", player.getTotalWins());
            entry.put("totalGames", player.getTotalGames());
            entry.put("rank", i + 1);
            leaderboard.add(entry);
        }

        if (leaderboard.isEmpty()) {
            Map<String, Object> demo = new HashMap<>();
            demo.put("name", "Нет игроков");
            demo.put("totalWins", 0);
            demo.put("totalGames", 0);
            demo.put("rank", 1);
            leaderboard.add(demo);
        }

        return leaderboard;
    }

    @PostMapping("/turn/start")
    public Map<String, Object> startTurn(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        String currentPlayerId = getCurrentPlayerId(userId);

        Map<String, Object> response = new HashMap<>();

        if (turnService.isGameFinished()) {
            response.put("success", false);
            response.put("message", "Игра уже завершена! Начните новую игру.");
            return response;
        }

        if (currentPlayerId == null) {
            response.put("success", false);
            response.put("message", "Игрок не выбран");
            return response;
        }

        Player turnOwner = turnService.getCurrentPlayer();
        if (turnOwner == null) {
            response.put("success", false);
            response.put("message", "Нет текущего игрока");
            return response;
        }

        if (!turnOwner.getId().equals(currentPlayerId)) {
            response.put("success", false);
            response.put("message", "Сейчас не ваш ход! Ход игрока: " + turnOwner.getName());
            return response;
        }

        if (turnService.getState() != TurnService.GameState.WAITING) {
            response.put("success", false);
            response.put("message", "Ход уже начат!");
            return response;
        }

        boolean success = turnService.startTurn(currentPlayerId);
        response.put("success", success);

        if (success) {
            Player current = turnService.getCurrentPlayer();
            response.put("message", "Ход начат! За этот ход можно захватить только ОДНУ клетку.");
            if (current != null) {
                response.put("gold", current.getResource(ResourceType.GOLD));
                response.put("wood", current.getResource(ResourceType.WOOD));
                response.put("food", current.getResource(ResourceType.FOOD));
                response.put("troops", current.getTotalTroops());
                response.put("cells", current.getCapturedCells().size());
            }
            gamePersistenceService.autoSave(userId);
        } else {
            response.put("message", "Не удалось начать ход");
        }

        return response;
    }

    @PostMapping("/turn/end")
    public Map<String, Object> endTurn(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        String currentPlayerId = getCurrentPlayerId(userId);

        Map<String, Object> response = new HashMap<>();

        if (turnService.isGameFinished()) {
            response.put("success", false);
            response.put("message", "Игра уже завершена!");
            return response;
        }

        if (currentPlayerId == null) {
            response.put("success", false);
            response.put("message", "Игрок не выбран");
            return response;
        }

        if (turnService.getState() != TurnService.GameState.PROCESSING) {
            response.put("success", false);
            response.put("message", "Ход не был начат!");
            return response;
        }

        boolean success = turnService.endTurn(currentPlayerId);
        response.put("success", success);

        if (success) {
            response.put("message", "Ход завершен!");
            Player nextPlayer = turnService.getCurrentPlayer();
            response.put("nextPlayer", nextPlayer != null ? nextPlayer.getName() : null);
            gamePersistenceService.autoSave(userId);
        } else {
            response.put("message", "Не удалось завершить ход");
        }

        return response;
    }

    @PostMapping("/attack")
    public Map<String, Object> attack(@RequestBody AttackRequest request, HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        String currentPlayerId = getCurrentPlayerId(userId);

        Map<String, Object> response = new HashMap<>();

        if (turnService.isGameFinished()) {
            response.put("status", "error");
            response.put("message", "Игра уже завершена!");
            return response;
        }

        if (currentPlayerId == null) {
            response.put("status", "error");
            response.put("message", "Игрок не выбран");
            return response;
        }

        Player turnOwner = turnService.getCurrentPlayer();
        if (turnOwner == null || !turnOwner.getId().equals(currentPlayerId)) {
            response.put("status", "error");
            response.put("message", "Сейчас не ваш ход!");
            return response;
        }

        if (turnService.getState() != TurnService.GameState.PROCESSING) {
            response.put("status", "error");
            response.put("message", "Сначала начните ход!");
            return response;
        }

        if (turnService.hasCapturedThisTurn()) {
            response.put("status", "error");
            response.put("message", "Вы уже захватили клетку в этом ходу! Завершите ход кнопкой 'Завершить ход'.");
            return response;
        }

        String cellId = request.x + ":" + request.y;
        Cell targetCell = gameWorld.getCell(cellId);

        if (targetCell == null) {
            response.put("status", "error");
            response.put("message", "Клетка не найдена");
            return response;
        }

        Player myPlayer = gameWorld.getPlayer(currentPlayerId);
        if (myPlayer == null) {
            response.put("status", "error");
            response.put("message", "Игрок не найден");
            return response;
        }

        List<Cell> playerCells = new ArrayList<>(myPlayer.getCapturedCells());
        playerCells.removeIf(Cell::isWater);

        if (playerCells.isEmpty()) {
            response.put("status", "error");
            response.put("message", "У вас нет клеток для атаки");
            return response;
        }

        boolean hasNeighbor = false;
        for (Cell playerCell : playerCells) {
            if (gameWorld.getNeighbors(playerCell).contains(targetCell)) {
                hasNeighbor = true;
                break;
            }
        }

        if (!hasNeighbor) {
            response.put("status", "error");
            response.put("message", "Нет соседних клеток для атаки! Можно атаковать только соседние клетки.");
            return response;
        }

        try {
            gameWorld.executeInstantAttack(playerCells, targetCell, currentPlayerId);
            turnService.registerCapture();
            gamePersistenceService.autoSave(userId);

            myPlayer = gameWorld.getPlayer(currentPlayerId);

            response.put("status", "ok");
            response.put("message", "Атака выполнена! За этот ход больше нельзя атаковать. Нажмите 'Завершить ход'.");
            response.put("troops", myPlayer.getTotalTroops());
            response.put("cells", myPlayer.getCapturedCells().size());
        } catch (Exception e) {
            System.err.println("Attack error: " + e.getMessage());
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @PostMapping("/build")
    public Map<String, Object> build(@RequestBody BuildRequest request, HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        String currentPlayerId = getCurrentPlayerId(userId);

        Map<String, Object> response = new HashMap<>();

        if (currentPlayerId == null) {
            response.put("success", false);
            response.put("message", "Игрок не выбран");
            return response;
        }

        Player myPlayer = gameWorld.getPlayer(currentPlayerId);
        if (myPlayer == null) {
            response.put("success", false);
            response.put("message", "Игрок не найден");
            return response;
        }

        String cellId = request.x + ":" + request.y;
        Cell cell = gameWorld.getCell(cellId);

        if (cell == null) {
            response.put("success", false);
            response.put("message", "Клетка не найдена");
            return response;
        }

        if (!currentPlayerId.equals(cell.getOwnerId())) {
            response.put("success", false);
            response.put("message", "Это не ваша клетка!");
            return response;
        }

        BuildingType buildingType;
        try {
            buildingType = BuildingType.valueOf(request.buildingType.toUpperCase());
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Неизвестный тип здания. Доступны: BARRACKS, PORT, SAWMILL, FARM");
            return response;
        }

        if (!cell.canBuild(buildingType)) {
            response.put("success", false);
            response.put("message", "Нельзя построить " + buildingType.getDisplayName() +
                    " на " + cell.getTerrain().name() + "! Можно строить только на: " + getBuildableTerrain(buildingType));
            return response;
        }

        int costGold = cell.getBuildCostGold(buildingType);
        int costWood = cell.getBuildCostWood(buildingType);

        if (myPlayer.getResource(ResourceType.GOLD) < costGold) {
            response.put("success", false);
            response.put("message", "Недостаточно золота! Нужно " + costGold + ", есть " + myPlayer.getResource(ResourceType.GOLD));
            return response;
        }

        if (myPlayer.getResource(ResourceType.WOOD) < costWood) {
            response.put("success", false);
            response.put("message", "Недостаточно дерева! Нужно " + costWood + ", есть " + myPlayer.getResource(ResourceType.WOOD));
            return response;
        }

        myPlayer.spendResource(ResourceType.GOLD, costGold);
        myPlayer.spendResource(ResourceType.WOOD, costWood);
        cell.setBuilding(buildingType);

        gamePersistenceService.autoSave(userId);

        response.put("success", true);
        response.put("message", "Построено: " + buildingType.getDisplayName() + "!");
        response.put("building", buildingType.toString());
        response.put("buildingName", buildingType.getDisplayName());
        response.put("goldBonus", buildingType.getGoldBonus());
        response.put("woodBonus", buildingType.getWoodBonus());
        response.put("foodBonus", buildingType.getFoodBonus());
        response.put("goldLeft", myPlayer.getResource(ResourceType.GOLD));
        response.put("woodLeft", myPlayer.getResource(ResourceType.WOOD));

        return response;
    }

    @GetMapping("/buildings")
    public List<Map<String, Object>> getAvailableBuildings() {
        List<Map<String, Object>> buildings = new ArrayList<>();
        for (BuildingType type : BuildingType.values()) {
            if (type != BuildingType.NONE) {
                Map<String, Object> building = new HashMap<>();
                building.put("type", type.toString());
                building.put("name", type.getDisplayName());
                building.put("costGold", type.getBuildCostGold());
                building.put("costWood", type.getBuildCostWood());
                building.put("goldBonus", type.getGoldBonus());
                building.put("woodBonus", type.getWoodBonus());
                building.put("foodBonus", type.getFoodBonus());
                building.put("canBuildOn", getBuildableTerrain(type));
                buildings.add(building);
            }
        }
        return buildings;
    }

    private String getBuildableTerrain(BuildingType buildingType) {
        switch (buildingType) {
            case BARRACKS: return "PLAIN";
            case PORT: return "WATER";
            case SAWMILL: return "FOREST";
            case FARM: return "PLAIN";
            default: return "unknown";
        }
    }

    @GetMapping("/debug")
    public Map<String, Object> debug(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        String currentPlayerId = getCurrentPlayerId(userId);

        Map<String, Object> debug = new HashMap<>();
        debug.put("currentUserId", userId);
        debug.put("currentPlayerId", currentPlayerId);
        debug.put("turnServiceState", turnService.getState().toString());
        debug.put("isGameFinished", turnService.isGameFinished());
        debug.put("winner", turnService.getWinner() != null ? turnService.getWinner().getName() : null);
        debug.put("currentPlayer", turnService.getCurrentPlayer() != null ?
                turnService.getCurrentPlayer().getName() : null);
        debug.put("canAttack", currentPlayerId != null && turnService.canAttack(currentPlayerId));
        debug.put("hasCapturedThisTurn", turnService.hasCapturedThisTurn());
        debug.put("playersCount", gameWorld.getPlayers().size());
        debug.put("cellsCount", gameWorld.getCells().size());
        debug.put("hasSavedGame", gamePersistenceService.hasSavedGame(userId));
        return debug;
    }

    @PostMapping("/reset")
    public Map<String, Object> resetGame(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

        gameWorld.reset();
        gameWorld.createWorld(10, 10);
        userCurrentPlayerIds.remove(userId);
        userStatisticsUpdated.remove(userId);
        turnService.reset();

        gamePersistenceService.clearSave(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Игра сброшена");
        return response;
    }

    @PostMapping("/set-user-id")
    public Map<String, Object> setUserId(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String userId = request.get("userId");
        if (userId != null && !userId.isEmpty()) {
            httpRequest.getSession().setAttribute("userId", userId);
            System.out.println("🔧 User ID принудительно установлен: " + userId);
            return Map.of("success", true, "userId", userId);
        }
        return Map.of("success", false, "message", "userId не указан");
    }

    private void updateWinnerStatistics(Player winner) {
        try {
            PlayerEntity playerEntity = statisticsService.getOrCreatePlayer(winner.getId(), winner.getName());
            int cellsCaptured = winner.getCapturedCells().size();

            statisticsService.updatePlayerStats(playerEntity, cellsCaptured, cellsCaptured * 10 + 100, true);

            String gameId = UUID.randomUUID().toString();
            statisticsService.addGameHistory(gameId, playerEntity, true, cellsCaptured, turnService.getTurnNumber());

            System.out.println("✅ ПОБЕДА! Статистика обновлена для: " + winner.getName());
            System.out.println("   - Всего побед: " + playerEntity.getTotalWins());
            System.out.println("   - Всего игр: " + playerEntity.getTotalGames());

        } catch (Exception e) {
            System.err.println("❌ Ошибка при обновлении статистики победителя: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateLoserStatistics(Player loser) {
        try {
            PlayerEntity playerEntity = statisticsService.getOrCreatePlayer(loser.getId(), loser.getName());
            int cellsCaptured = loser.getCapturedCells().size();

            statisticsService.updatePlayerStats(playerEntity, cellsCaptured, cellsCaptured * 10, false);

            String gameId = UUID.randomUUID().toString();
            statisticsService.addGameHistory(gameId, playerEntity, false, cellsCaptured, turnService.getTurnNumber());

            System.out.println("📊 Статистика обновлена для проигравшего: " + loser.getName());

        } catch (Exception e) {
            System.err.println("❌ Ошибка при обновлении статистики проигравшего: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setCurrentPlayerId(String userId, String playerId) {
        userCurrentPlayerIds.put(userId, playerId);
        userStatisticsUpdated.put(userId, false);
        System.out.println("🎮 Пользователь " + userId + " теперь играет за " + playerId);
    }

    static class AttackRequest {
        public int x;
        public int y;
        public int troops;
    }

    static class BuildRequest {
        public int x;
        public int y;
        public String buildingType;
    }
}