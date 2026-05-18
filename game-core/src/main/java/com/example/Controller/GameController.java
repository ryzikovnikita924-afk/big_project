package com.example.Controller;

import com.example.entity.PlayerEntity;
import com.example.service.GamePersistenceService;
import com.example.service.StatisticsService;
import com.example.world.GameWorld;
import com.example.service.TurnService;
import com.example.model.Player;
import com.example.model.Cell;
import com.example.model.ResourceType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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
    private boolean gameInitialized = false;

    public GameController(GameWorld gameWorld,
                          TurnService turnService,
                          StatisticsService statisticsService,
                          GamePersistenceService gamePersistenceService) {
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        this.statisticsService = statisticsService;
        this.gamePersistenceService = gamePersistenceService;
        this.currentGameId = UUID.randomUUID().toString();
    }

    private void initGameIfNeeded() {
        if (gameInitialized) {
            return;
        }

        if (!gameWorld.getCells().isEmpty()) {
            System.out.println("Мир уже инициализирован");
            gameInitialized = true;
            return;
        }

        gameWorld.createWorld(10, 10);


        gameWorld.start();
        gameInitialized = true;
        System.out.println("✅ Игровой мир создан!");
    }


    public void addPlayerToGame(String playerId, String playerName) {

        if (gameWorld.getPlayer(playerId) != null) {
            System.out.println("Игрок " + playerName + " уже в игре");
            return;
        }


        Player gamePlayer = new Player(playerName);
        gamePlayer.setId(playerId);


        int startX = 2, startY = 2;
        if (!gameWorld.getPlayers().isEmpty()) {
            // Если есть другие игроки, ставим нового на противоположную сторону
            startX = 7;
            startY = 7;
        }

        gameWorld.addPlayer(gamePlayer, startX, startY);

        if (allPlayers == null) {
            allPlayers = new ArrayList<>();
        }
        allPlayers.add(gamePlayer);


        if (allPlayers.size() == 1) {
            turnService.initialize(allPlayers);
            currentPlayerId = gamePlayer.getId();
        } else {

            turnService.updatePlayers(allPlayers);
        }

        System.out.println("✅ Игрок " + playerName + " добавлен в игру");
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/api/players")
    @ResponseBody
    public List<Map<String, Object>> getPlayers() {
        initGameIfNeeded();

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
        initGameIfNeeded();

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
        initGameIfNeeded();

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
    @GetMapping("/api/auth/check")
    @ResponseBody
    public Map<String, Object> checkAuth(HttpServletRequest request) {
        String user = request.getHeader("X-Forwarded-User");
        String email = request.getHeader("X-Forwarded-Email");

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", user != null && !user.isEmpty());
        response.put("username", user);
        response.put("email", email);
        return response;
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


        String toId = request.toX + ":" + request.toY;
        Cell toCell = gameWorld.getCell(toId);

        if (toCell == null) {
            return Map.of("status", "error", "message", "Клетка не найдена!");
        }


        try {
            gameWorld.executeInstantAttack(playerCells, toCell, currentPlayerId);
            gamePersistenceService.autoSave();
            return Map.of("status", "ok", "message", "⚔️ Атака выполнена!");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @PostMapping("/api/start-turn")
    @ResponseBody
    public Map<String, Object> startTurn() {
        if (currentPlayerId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка: игрок не выбран");
            return response;
        }

        Player turnOwner = turnService.getCurrentPlayer();
        if (turnOwner == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка: нет текущего игрока");
            return response;
        }

        if (!turnOwner.getId().equals(currentPlayerId)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Сейчас не ваш ход! Сейчас ход игрока: " + turnOwner.getName());
            response.put("currentTurnPlayer", turnOwner.getName());
            return response;
        }

        if (turnService.getState() != TurnService.GameState.WAITING) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ход уже начат! Текущий статус: " + turnService.getState());
            return response;
        }

        boolean success = turnService.startTurn(currentPlayerId);
        Map<String, Object> response = new HashMap<>();
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
            gamePersistenceService.autoSave();
        } else {
            response.put("message", "⏳ Не удалось начать ход. Попробуйте еще раз.");
        }
        return response;
    }

    @PostMapping("/api/end-turn")
    @ResponseBody
    public Map<String, Object> endTurn() {
        if (currentPlayerId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка: игрок не выбран");
            return response;
        }

        if (turnService.getState() != TurnService.GameState.PROCESSING) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ход не был начат! Текущий статус: " + turnService.getState());
            return response;
        }

        boolean success = turnService.endTurn(currentPlayerId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            response.put("success", true);
            response.put("message", "✅ Ход завершен!");

            Player nextPlayer = turnService.getCurrentPlayer();
            response.put("nextPlayer", nextPlayer != null ? nextPlayer.getName() : null);
            response.put("nextPlayerId", nextPlayer != null ? nextPlayer.getId() : null);
            response.put("gameState", turnService.getState().toString());

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
        initGameIfNeeded();

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
            state.put("myCells", myPlayer.getcapturedCells().size());
        }

        return state;
    }

    private String getCurrentPlayerName() {
        Player player = gameWorld.getPlayer(currentPlayerId);
        return player != null ? player.getName() : "Не выбран";
    }

    static class AttackRequest {
        public int toX, toY, troops;
    }
}