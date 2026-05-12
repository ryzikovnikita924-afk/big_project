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
    private String currentGameId;

    public GamePersistenceService(RedisGameStateService redisService,
                                  StatisticsService statisticsService,
                                  GameWorld gameWorld,
                                  TurnService turnService) {
        this.redisService = redisService;
        this.statisticsService = statisticsService;
        this.gameWorld = gameWorld;
        this.turnService = turnService;
        this.currentGameId = UUID.randomUUID().toString();
    }

    public void saveCurrentGameState() {
        GameStateSnapshot snapshot = createSnapshot();
        redisService.saveCurrentGame(snapshot);
    }

    public boolean loadCurrentGameState() {
        GameStateSnapshot snapshot = redisService.loadCurrentGame();
        if (snapshot == null) return false;
        restoreFromSnapshot(snapshot);
        return true;
    }

    public void finishGame(Player winner) {
        for (Player player : gameWorld.getPlayers().values()) {
            int cellsCaptured = player.getCapturedCellIds().size();
            int troopsKilled = calculateTroopsKilled(player);
            boolean isWinner = winner != null && winner.getId().equals(player.getId());
            int turnsPlayed = turnService.getTurnNumber();

            com.example.entity.PlayerEntity playerEntity = statisticsService.getPlayerById(player.getId());
            if (playerEntity != null) {
                statisticsService.updatePlayerStats(playerEntity, cellsCaptured, troopsKilled, 0, isWinner);
                statisticsService.addGameHistory(currentGameId, playerEntity, isWinner, cellsCaptured, troopsKilled, turnsPlayed);
            }
        }

        statisticsService.endGameSession(currentGameId, winner, turnService.getTurnNumber());
        redisService.deleteCurrentGame();
        System.out.println("🏁 Игра завершена! Победитель: " + (winner != null ? winner.getName() : "Ничья"));
    }

    public void newGame(List<Player> players) {
        currentGameId = UUID.randomUUID().toString();
        for (Player player : players) {
            statisticsService.getOrCreatePlayer(player.getId(), player.getName());
        }
        redisService.deleteCurrentGame();
    }

    private GameStateSnapshot createSnapshot() {
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
        snapshot.setGameId(currentGameId);

        return snapshot;
    }

    private void restoreFromSnapshot(GameStateSnapshot snapshot) {
        gameWorld.clear();
        for (CellSnapshot cellSnapshot : snapshot.getCells().values()) {
            Cell cell = new Cell(cellSnapshot.getX(), cellSnapshot.getY(),
                    TerrainType.valueOf(cellSnapshot.getTerrain()));
            cell.setOwnerId(cellSnapshot.getOwnerId());
            cell.setTroopsCount(cellSnapshot.getTroopsCount());
            cell.setLevel(cellSnapshot.getLevel());
            gameWorld.addCell(cell);
        }

        List<Player> players = new ArrayList<>();
        for (PlayerSnapshot playerSnapshot : snapshot.getPlayers().values()) {
            Player player = restorePlayer(playerSnapshot);
            gameWorld.addPlayerDirect(player);
            players.add(player);

            com.example.entity.PlayerEntity existingEntity = statisticsService.getPlayerById(player.getId());
            if (existingEntity == null) {
                statisticsService.getOrCreatePlayer(player.getId(), player.getName());
            }
        }

        turnService.initializeFromSnapshot(players,
                snapshot.getCurrentTurnPlayerId(),
                snapshot.getTurnNumber(),
                TurnService.GameState.valueOf(snapshot.getGameState()));

        currentGameId = snapshot.getGameId();
    }

    private Player restorePlayer(PlayerSnapshot snapshot) {
        Player player = new Player(snapshot.getName());
        player.setId(snapshot.getId());
        for (Map.Entry<String, Integer> resource : snapshot.getResources().entrySet()) {
            player.addResource(ResourceType.valueOf(resource.getKey()), resource.getValue());
        }
        for (String cellId : snapshot.getCapturedCellIds()) {
            player.addCell(cellId);
        }
        player.setTotalTroops(snapshot.getTotalTroops());
        player.setVictories(snapshot.getVictories());
        return player;
    }

    private int calculateTroopsKilled(Player player) {
        return 0;
    }

    public void autoSave() {
        saveCurrentGameState();
        System.out.println("💾 Автосохранение в Redis выполнено");
    }

    public String getCurrentGameId() {
        return currentGameId;
    }
}