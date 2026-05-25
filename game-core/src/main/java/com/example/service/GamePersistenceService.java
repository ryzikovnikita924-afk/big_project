package com.example.service;

import com.example.model.*;
import com.example.snapshot.CellSnapshot;
import com.example.snapshot.GameStateSnapshot;
import com.example.snapshot.PlayerSnapshot;
import com.example.world.GameWorld;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GamePersistenceService {

    private final RedisGameStateService redisService;
    private final StatisticsService statisticsService;
    private final GameWorld gameWorld;
    private final TurnService turnService;

    public GamePersistenceService(RedisGameStateService redisService,
                                  StatisticsService statisticsService,
                                  GameWorld gameWorld,
                                  TurnService turnService) {
        this.redisService = redisService;
        this.statisticsService = statisticsService;
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        System.out.println("🎮 GamePersistenceService инициализирован");
    }

    public void saveCurrentGameState(String userId) {
        try {
            System.out.println("\n🔵 СОХРАНЕНИЕ для пользователя: " + userId);
            GameStateSnapshot snapshot = createSnapshot(userId);
            if (snapshot == null) {
                System.err.println("❌ Не удалось создать snapshot!");
                return;
            }
            redisService.saveCurrentGame(userId, snapshot);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при сохранении: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public GameStateSnapshot loadCurrentGameState(String userId) {
        try {
            return redisService.loadCurrentGame(userId);
        } catch (Exception e) {
            System.err.println("❌ Ошибка при загрузке: " + e.getMessage());
            return null;
        }
    }

    private GameStateSnapshot createSnapshot(String userId) {
        try {
            GameStateSnapshot snapshot = new GameStateSnapshot();

            Map<String, CellSnapshot> cellSnapshots = new HashMap<>();
            for (Map.Entry<String, Cell> entry : gameWorld.getCells().entrySet()) {
                cellSnapshots.put(entry.getKey(), new CellSnapshot(entry.getValue()));
            }
            snapshot.setCells(cellSnapshots);

            Map<String, PlayerSnapshot> playerSnapshots = new HashMap<>();
            for (Map.Entry<String, Player> entry : gameWorld.getPlayers().entrySet()) {
                playerSnapshots.put(entry.getKey(), new PlayerSnapshot(entry.getValue()));
            }
            snapshot.setPlayers(playerSnapshots);

            Player currentPlayer = turnService.getCurrentPlayer();
            snapshot.setCurrentTurnPlayerId(currentPlayer != null ? currentPlayer.getId() : null);
            snapshot.setTurnNumber(turnService.getTurnNumber());
            snapshot.setGameState(turnService.getState().toString());
            snapshot.setGameId(userId + "_" + System.currentTimeMillis());
            snapshot.setTimestamp(System.currentTimeMillis());

            return snapshot;
        } catch (Exception e) {
            System.err.println("❌ Ошибка создания snapshot: " + e.getMessage());
            return null;
        }
    }

    public void autoSave(String userId) {
        System.out.println("\n💾 АВТОСОХРАНЕНИЕ для пользователя: " + userId);
        saveCurrentGameState(userId);
    }

    public void clearSave(String userId) {
        redisService.deleteCurrentGame(userId);
    }

    public boolean hasSavedGame(String userId) {
        return redisService.hasCurrentGame(userId);
    }
}