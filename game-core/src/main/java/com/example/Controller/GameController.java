package com.example.Controller;

import com.example.entity.PlayerEntity;
import com.example.model.*;
import com.example.service.GamePersistenceService;
import com.example.service.TurnService;
import com.example.world.GameWorld;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameWorld gameWorld;
    private final TurnService turnService;
    private final GamePersistenceService gamePersistenceService;
    private String currentPlayerId;

    public GameController(GameWorld gameWorld,
                          TurnService turnService,
                          GamePersistenceService gamePersistenceService) {
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        this.gamePersistenceService = gamePersistenceService;
    }

    @GetMapping("/map")
    public Map<String, Object> getMap() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> cells = new ArrayList<>();

        System.out.println("=== GET /api/game/map ===");
        System.out.println("Total cells in world: " + gameWorld.getCells().size());

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
            playersList.add(playerData);
        }
        result.put("players", playersList);

        return result;
    }

    @GetMapping("/state")
    public Map<String, Object> getGameState() {
        System.out.println("=== GET /api/game/state ===");

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
            return state;
        }

        Player turnOwner = turnService.getCurrentPlayer();
        Player winner = gameWorld.getWinner();
        Player myPlayer = currentPlayerId != null ? gameWorld.getPlayer(currentPlayerId) : null;

        System.out.println("Current player ID from TurnService: " + (turnOwner != null ? turnOwner.getId() : "null"));
        System.out.println("My player ID from GameController: " + currentPlayerId);
        System.out.println("My player object: " + (myPlayer != null ? myPlayer.getName() : "null"));
        System.out.println("My player cells: " + (myPlayer != null ? myPlayer.getCapturedCells().size() : 0));
        System.out.println("My player troops: " + (myPlayer != null ? myPlayer.getTotalTroops() : 0));

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
        } else {
            state.put("myGold", 0);
            state.put("myWood", 0);
            state.put("myFood", 0);
            state.put("myTroops", 0);
            state.put("myCells", 0);
        }

        return state;
    }

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        System.out.println("=== GET /api/game/leaderboard ===");

        List<Map<String, Object>> leaderboard = new ArrayList<>();

        List<Player> sortedPlayers = new ArrayList<>(gameWorld.getPlayers().values());
        sortedPlayers.sort((p1, p2) -> Integer.compare(p2.getTotalWins(), p1.getTotalWins()));

        for (int i = 0; i < Math.min(10, sortedPlayers.size()); i++) {
            Player player = sortedPlayers.get(i);
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
    public Map<String, Object> startTurn() {
        System.out.println("=== POST /api/game/turn/start ===");

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

        System.out.println("Comparing IDs - TurnOwner: " + turnOwner.getId() + ", Current: " + currentPlayerId);

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
        } else {
            response.put("message", "Не удалось начать ход");
        }

        return response;
    }

    @PostMapping("/turn/end")
    public Map<String, Object> endTurn() {
        System.out.println("=== POST /api/game/turn/end ===");

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
        } else {
            response.put("message", "Не удалось завершить ход");
        }

        return response;
    }

    @PostMapping("/attack")
    public Map<String, Object> attack(@RequestBody AttackRequest request) {
        System.out.println("=== POST /api/game/attack ===");
        System.out.println("Attack target: [" + request.x + "," + request.y + "]");

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

        System.out.println("Player cells count: " + playerCells.size());
        System.out.println("Player troops: " + myPlayer.getTotalTroops());

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
            gamePersistenceService.autoSave();

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
    public Map<String, Object> build(@RequestBody BuildRequest request) {
        System.out.println("=== POST /api/game/build ===");
        System.out.println("Build on cell: [" + request.x + "," + request.y + "] type: " + request.buildingType);

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
        gamePersistenceService.autoSave();

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
    public Map<String, Object> debug() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("currentPlayerId", currentPlayerId);
        debug.put("turnServiceState", turnService.getState().toString());
        debug.put("isGameFinished", turnService.isGameFinished());
        debug.put("winner", turnService.getWinner() != null ? turnService.getWinner().getName() : null);
        debug.put("currentPlayer", turnService.getCurrentPlayer() != null ?
                turnService.getCurrentPlayer().getName() : null);
        debug.put("currentPlayerIdFromTurn", turnService.getCurrentPlayer() != null ?
                turnService.getCurrentPlayer().getId() : null);
        debug.put("canAttack", currentPlayerId != null && turnService.canAttack(currentPlayerId));
        debug.put("hasCapturedThisTurn", turnService.hasCapturedThisTurn());
        debug.put("playersCount", gameWorld.getPlayers().size());
        debug.put("cellsCount", gameWorld.getCells().size());
        debug.put("myPlayerExists", currentPlayerId != null && gameWorld.getPlayer(currentPlayerId) != null);
        return debug;
    }

    @PostMapping("/reset")
    public Map<String, Object> resetGame() {
        System.out.println("=== POST /api/game/reset ===");

        gameWorld.reset();
        gameWorld.createWorld(10, 10);

        this.currentPlayerId = null;

        turnService.reset();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Игра сброшена");
        return response;
    }

    public void setCurrentPlayerId(String playerId) {
        this.currentPlayerId = playerId;
        System.out.println("Current player ID set to: " + playerId);
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
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