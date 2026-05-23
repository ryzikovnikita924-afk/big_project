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
    private final com.example.Controller.GameController gameController;
    private final com.example.Controller.AIController aiController;

    public GameSessionController(GameSessionService gameSessionService,
                                 GameWorld gameWorld,
                                 TurnService turnService,
                                 AIPlayer aiPlayer,
                                 com.example.Controller.GameController gameController,
                                 com.example.Controller.AIController aiController) {
        this.gameSessionService = gameSessionService;
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        this.aiPlayer = aiPlayer;
        this.gameController = gameController;
        this.aiController = aiController;
    }

    @PostMapping("/start-vs-ai")
    public Map<String, Object> startGameVsAI(@RequestBody Map<String, String> request) {
        String playerId = request.get("playerId");
        String playerName = request.get("playerName");
        String difficulty = request.getOrDefault("difficulty", "BALANCED");

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== STARTING VS AI GAME ===");
            System.out.println("Player ID: " + playerId);
            System.out.println("Player Name: " + playerName);
            System.out.println("Difficulty: " + difficulty);

            gameWorld.reset();
            gameWorld.createWorld(10, 10);

            System.out.println("World created, cells: " + gameWorld.getCells().size());

            List<com.example.model.Player> playersList = new ArrayList<>();

            com.example.model.Player humanPlayer = new com.example.model.Player(playerId, playerName);
            gameWorld.addPlayer(humanPlayer, 2, 2);
            playersList.add(humanPlayer);
            System.out.println("Human player added: " + humanPlayer.getId());
            System.out.println("Human player cells: " + humanPlayer.getCapturedCells().size());

            String aiId = "ai_" + UUID.randomUUID().toString();
            com.example.model.Player aiPlayerModel = new com.example.model.Player(aiId, "AI (" + difficulty + ")");
            gameWorld.addPlayer(aiPlayerModel, 7, 7);
            playersList.add(aiPlayerModel);
            System.out.println("AI player added: " + aiPlayerModel.getId());
            System.out.println("AI player cells: " + aiPlayerModel.getCapturedCells().size());

            turnService.initialize(playersList);
            System.out.println("TurnService initialized with " + playersList.size() + " players");
            System.out.println("Current player: " + turnService.getCurrentPlayer().getName());
            System.out.println("Current player ID: " + turnService.getCurrentPlayer().getId());

            gameController.setCurrentPlayerId(playerId);
            System.out.println("Current player set in GameController: " + playerId);

            try {
                AIPlayer.AITactic tactic = AIPlayer.AITactic.valueOf(difficulty.toUpperCase());
                aiPlayer.setTactic(tactic);
                System.out.println("AI Tactic set: " + tactic);
            } catch (IllegalArgumentException e) {
                aiPlayer.setTactic(AIPlayer.AITactic.BALANCED);
                System.out.println("AI Tactic set to BALANCED");
            }

            aiController.setAiPlayerId(aiId);
            aiController.enableAI(true);
            System.out.println("AI Controller configured with ID: " + aiId);

            gameSessionService.createSession(playerId, aiId, difficulty);
            System.out.println("Session created: " + gameSessionService.getCurrentGameId());

            response.put("success", true);
            response.put("message", "Игра против AI начата!");
            response.put("gameId", gameSessionService.getCurrentGameId());
            response.put("aiId", aiId);
            response.put("playerId", playerId);
            response.put("currentTurn", turnService.getCurrentPlayer().getName());

            System.out.println("=== GAME STARTED SUCCESSFULLY ===");

        } catch (Exception e) {
            System.err.println("Error starting game: " + e.getMessage());
            e.printStackTrace();
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