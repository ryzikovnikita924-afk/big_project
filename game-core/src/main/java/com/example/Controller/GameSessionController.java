package com.example.Controller;

import com.example.engine.AIPlayer;
import com.example.service.GameSessionService;
import com.example.service.TurnService;
import com.example.world.GameWorld;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/game")
public class GameSessionController {

    private final GameSessionService gameSessionService;
    private final GameWorld gameWorld;
    private final TurnService turnService;
    private final AIPlayer aiPlayer;

    public GameSessionController(GameSessionService gameSessionService,
                                 GameWorld gameWorld,
                                 TurnService turnService,
                                 AIPlayer aiPlayer) {
        this.gameSessionService = gameSessionService;
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        this.aiPlayer = aiPlayer;
    }

    @PostMapping("/start-vs-ai")
    public Map<String, Object> startGameVsAI(@RequestBody Map<String, String> request) {
        String playerId = request.get("playerId");
        String playerName = request.get("playerName");
        String difficulty = request.getOrDefault("difficulty", "BALANCED");

        Map<String, Object> response = new HashMap<>();

        try {
            // Очищаем мир для новой игры
            gameWorld.reset();

            // Создаем мир заново
            gameWorld.createWorld(10, 10);

            // Добавляем игрока
            gameWorld.addPlayer(new com.example.model.Player(playerName), 2, 2);

            // Добавляем AI
            String aiId = "ai_" + UUID.randomUUID().toString();
            com.example.model.Player aiPlayerModel = new com.example.model.Player("AI (" + difficulty + ")");
            aiPlayerModel.setId(aiId);
            gameWorld.addPlayer(aiPlayerModel, 7, 7);

            // Настраиваем AI тактику
            try {
                AIPlayer.AITactic tactic = AIPlayer.AITactic.valueOf(difficulty.toUpperCase());
                aiPlayer.setTactic(tactic);
            } catch (IllegalArgumentException e) {
                aiPlayer.setTactic(AIPlayer.AITactic.BALANCED);
            }

            // Сохраняем сессию
            gameSessionService.createSession(playerId, aiId, difficulty);

            response.put("success", true);
            response.put("message", "Игра против AI начата!");
            response.put("gameId", gameSessionService.getCurrentGameId());
            response.put("aiId", aiId);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/sessions")
    public List<Map<String, Object>> getActiveSessions() {
        return gameSessionService.getActiveSessions();
    }

    @PostMapping("/reset")
    public Map<String, Object> resetGame() {
        gameWorld.reset();
        gameWorld.createWorld(10, 10);
        gameSessionService.clearCurrentSession();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Игра сброшена");
        return response;
    }
}