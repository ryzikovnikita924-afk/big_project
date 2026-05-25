package com.example.Controller;

import com.example.engine.AIPlayer;
import com.example.service.GamePersistenceService;
import com.example.service.GameSessionService;
import com.example.service.TurnService;
import com.example.world.GameWorld;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/game")
public class GameSessionController {

    private final GameSessionService gameSessionService;
    private final GameWorld gameWorld;
    private final TurnService turnService;
    private final AIPlayer aiPlayer;
    private final GameController gameController;
    private final AIController aiController;
    private final GamePersistenceService gamePersistenceService;

    public GameSessionController(GameSessionService gameSessionService,
                                 GameWorld gameWorld,
                                 TurnService turnService,
                                 AIPlayer aiPlayer,
                                 GameController gameController,
                                 AIController aiController,
                                 GamePersistenceService gamePersistenceService) {
        this.gameSessionService = gameSessionService;
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        this.aiPlayer = aiPlayer;
        this.gameController = gameController;
        this.aiController = aiController;
        this.gamePersistenceService = gamePersistenceService;
    }

    private String getUserId(HttpServletRequest request) {
        // Сначала пытаемся получить userId из заголовка (от фронтенда)
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            System.out.println("📌 Получен userId из заголовка: " + userId);
            return userId;
        }

        // Если нет в заголовке, берем из сессии
        userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            request.getSession().setAttribute("userId", userId);
        }
        System.out.println("📌 Получен userId из сессии: " + userId);
        return userId;
    }

    @PostMapping("/start-vs-ai")
    public Map<String, Object> startGameVsAI(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        // Приоритет: из тела запроса, потом из заголовка, потом из сессии
        String userId = request.get("userId");
        if (userId == null || userId.isEmpty()) {
            userId = getUserId(httpRequest);
        }

        String playerId = request.get("playerId");
        String playerName = request.get("playerName");
        String difficulty = request.getOrDefault("difficulty", "BALANCED");

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== STARTING VS AI GAME for user: " + userId + " ===");
            System.out.println("Player ID: " + playerId);
            System.out.println("Player Name: " + playerName);
            System.out.println("Difficulty: " + difficulty);

            gameWorld.reset();
            gameWorld.createWorld(10, 10);

            List<com.example.model.Player> playersList = new ArrayList<>();

            com.example.model.Player humanPlayer = new com.example.model.Player(playerId, playerName);
            gameWorld.addPlayer(humanPlayer, 2, 2);
            playersList.add(humanPlayer);

            String aiId = "ai_" + UUID.randomUUID().toString();
            com.example.model.Player aiPlayerModel = new com.example.model.Player(aiId, "AI (" + difficulty + ")");
            gameWorld.addPlayer(aiPlayerModel, 7, 7);
            playersList.add(aiPlayerModel);

            turnService.initialize(playersList);

            gameController.setCurrentPlayerId(userId, playerId);

            try {
                AIPlayer.AITactic tactic = AIPlayer.AITactic.valueOf(difficulty.toUpperCase());
                aiPlayer.setTactic(tactic);
            } catch (IllegalArgumentException e) {
                aiPlayer.setTactic(AIPlayer.AITactic.BALANCED);
            }

            aiController.setAiPlayerId(aiId);
            aiController.enableAI(true);

            gameSessionService.createSession(userId, playerId, aiId, difficulty);

            gamePersistenceService.autoSave(userId);

            response.put("success", true);
            response.put("message", "Игра против AI начата!");
            response.put("gameId", gameSessionService.getCurrentGameId(userId));
            response.put("aiId", aiId);
            response.put("playerId", playerId);
            response.put("currentTurn", turnService.getCurrentPlayer().getName());
            response.put("userId", userId);

            System.out.println("=== GAME STARTED SUCCESSFULLY for user " + userId + " ===");

        } catch (Exception e) {
            System.err.println("Error starting game: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }

        return response;
    }

    @PostMapping("/load-session")
    public Map<String, Object> loadSession(HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== LOAD SESSION for user: " + userId + " ===");

            if (!gameSessionService.hasSession(userId)) {
                response.put("success", false);
                response.put("message", "Нет сохраненной сессии для пользователя");
                return response;
            }

            GameSessionService.GameSession session = gameSessionService.getSession(userId);
            if (session == null) {
                response.put("success", false);
                response.put("message", "Сессия не найдена");
                return response;
            }

            var loadedSnapshot = gamePersistenceService.loadCurrentGameState(userId);

            if (loadedSnapshot != null) {
                response.put("success", true);
                response.put("message", "Сохраненная игра загружена");
                response.put("gameId", session.getGameId());
                response.put("turn", loadedSnapshot.getTurnNumber());
            } else {
                response.put("success", false);
                response.put("message", "Нет сохраненных данных");
            }

        } catch (Exception e) {
            System.err.println("Error loading session: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/sessions")
    public List<Map<String, Object>> getActiveSessions() {
        return gameSessionService.getActiveSessions();
    }
}