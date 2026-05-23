package com.example.Controller;

import com.example.engine.AIPlayer;
import com.example.service.TurnService;
import com.example.world.GameWorld;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIPlayer aiPlayer;
    private final GameWorld gameWorld;
    private final TurnService turnService;
    private boolean aiEnabled = true;
    private String aiPlayerId = null;
    private Thread aiThread;

    public AIController(AIPlayer aiPlayer, GameWorld gameWorld, TurnService turnService) {
        this.aiPlayer = aiPlayer;
        this.gameWorld = gameWorld;
        this.turnService = turnService;

        // Запускаем AI поток
        startAIThread();
    }

    public void setAiPlayerId(String aiPlayerId) {
        this.aiPlayerId = aiPlayerId;
        System.out.println("🤖 AI Player ID установлен: " + aiPlayerId);
    }

    public void enableAI(boolean enabled) {
        this.aiEnabled = enabled;
        System.out.println("🤖 AI " + (enabled ? "включен" : "выключен"));
    }

    @PostMapping("/enable")
    public Map<String, Object> enableAI(@RequestBody Map<String, Object> request) {
        aiEnabled = (Boolean) request.getOrDefault("enabled", true);
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", aiEnabled);
        response.put("message", aiEnabled ? "AI включен" : "AI выключен");
        return response;
    }

    @PostMapping("/set-tactic")
    public Map<String, Object> setTactic(@RequestBody Map<String, String> request) {
        String tacticStr = request.get("tactic");
        try {
            AIPlayer.AITactic tactic = AIPlayer.AITactic.valueOf(tacticStr.toUpperCase());
            aiPlayer.setTactic(tactic);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tactic", tactic.toString());
            return response;
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Неизвестная тактика. Доступны: AGGRESSIVE, DEFENSIVE, ECONOMIC, BALANCED, RANDOM");
            return response;
        }
    }

    @GetMapping("/tactics")
    public List<String> getTactics() {
        return Arrays.asList("AGGRESSIVE", "DEFENSIVE", "ECONOMIC", "BALANCED", "RANDOM");
    }

    @PostMapping("/set-player")
    public Map<String, Object> setAPlayer(@RequestBody Map<String, String> request) {
        String playerName = request.get("playerName");
        for (var player : gameWorld.getPlayers().values()) {
            if (player.getName().equals(playerName)) {
                aiPlayerId = player.getId();
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "AI будет играть за: " + playerName);
                return response;
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Игрок не найден");
        return response;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", aiEnabled);
        status.put("aiPlayerId", aiPlayerId);
        status.put("tactic", aiPlayer.getCurrentTactic().toString());
        status.put("currentTurn", turnService.getCurrentPlayer() != null ?
                turnService.getCurrentPlayer().getName() : "none");
        return status;
    }


    private void startAIThread() {
        aiThread = new Thread(() -> {
            System.out.println("🤖 AI поток запущен");
            while (true) {
                try {
                    Thread.sleep(2000); // Проверяем каждые 2 секунды

                    if (!aiEnabled || aiPlayerId == null) {
                        continue;
                    }

                    // Проверяем, что сейчас ход AI
                    var currentPlayer = turnService.getCurrentPlayer();
                    if (currentPlayer != null &&
                            currentPlayer.getId().equals(aiPlayerId) &&
                            turnService.getState() == TurnService.GameState.WAITING) {

                        System.out.println("🤖 AI обнаружил свой ход! Начинает...");

                        // Небольшая задержка перед ходом AI
                        Thread.sleep(1000);

                        // AI делает ход
                        aiPlayer.makeTurn(aiPlayerId);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("🤖 AI поток остановлен");
                    break;
                } catch (Exception e) {
                    System.err.println("Ошибка в AI потоке: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        aiThread.setDaemon(true);
        aiThread.start();
    }
}