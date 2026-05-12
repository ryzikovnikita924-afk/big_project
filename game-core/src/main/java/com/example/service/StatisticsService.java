package com.example.service;

import com.example.entity.PlayerEntity;
import com.example.entity.GameHistoryEntity;
import com.example.repository.PlayerRepository;
import com.example.repository.GameHistoryRepository;
import com.example.model.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatisticsService {

    private final PlayerRepository playerRepository;
    private final GameHistoryRepository gameHistoryRepository;

    public StatisticsService(PlayerRepository playerRepository,
                             GameHistoryRepository gameHistoryRepository) {
        this.playerRepository = playerRepository;
        this.gameHistoryRepository = gameHistoryRepository;
    }

    public PlayerEntity getPlayerByEmail(String email) {
        return playerRepository.findByEmail(email).orElse(null);
    }

    public PlayerEntity getPlayerById(String playerId) {
        return playerRepository.findById(playerId).orElse(null);
    }

    public List<PlayerEntity> getLeaderboard() {
        return playerRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalWins(), a.getTotalWins()))
                .limit(10)
                .toList();
    }

    public List<Map<String, Object>> getPlayerHistory(String playerId) {
        List<GameHistoryEntity> history = gameHistoryRepository.findByPlayerId(playerId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (GameHistoryEntity game : history) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("gameId", game.getGameId());
            entry.put("isWinner", game.isWinner());
            entry.put("cellsCaptured", game.getCellsCaptured());
            entry.put("troopsKilled", game.getTroopsKilled());
            entry.put("finalScore", game.getFinalScore());
            entry.put("playedAt", game.getPlayedAt());
            result.add(entry);
        }

        return result;
    }

    @Transactional
    public void updatePlayerStats(PlayerEntity player, int cellsCaptured, int troopsKilled, int score, boolean isWinner) {
        player.setTotalGames(player.getTotalGames() + 1);
        player.setTotalCellsCaptured(player.getTotalCellsCaptured() + cellsCaptured);
        player.setTotalTroopsKilled(player.getTotalTroopsKilled() + troopsKilled);
        player.setTotalScore(player.getTotalScore() + score);
        player.setLastPlayed(LocalDateTime.now());

        if (isWinner) {
            player.setTotalWins(player.getTotalWins() + 1);
        }

        playerRepository.save(player);
    }

    @Transactional
    public void addGameHistory(String gameId, PlayerEntity player, boolean isWinner,
                               int cellsCaptured, int troopsKilled, int turnsPlayed) {
        GameHistoryEntity history = new GameHistoryEntity();
        history.setPlayer(player);
        history.setGameId(gameId);
        history.setPlayerName(player.getName());
        history.setWinner(isWinner);
        history.setCellsCaptured(cellsCaptured);
        history.setTroopsKilled(troopsKilled);
        history.setTurnsPlayed(turnsPlayed);
        history.setFinalScore(calculateScore(cellsCaptured, troopsKilled, isWinner));
        history.setPlayedAt(LocalDateTime.now());
        history.setDurationMinutes(0);

        gameHistoryRepository.save(history);
    }

    @Transactional
    public void addGameHistory(String gameId, PlayerEntity player, String playerColor,
                               boolean isWinner, int cellsCaptured, int troopsKilled,
                               int turnsPlayed, int durationMinutes) {
        GameHistoryEntity history = new GameHistoryEntity();
        history.setPlayer(player);
        history.setGameId(gameId);
        history.setPlayerName(player.getName());
        history.setPlayerColor(playerColor);
        history.setWinner(isWinner);
        history.setCellsCaptured(cellsCaptured);
        history.setTroopsKilled(troopsKilled);
        history.setTurnsPlayed(turnsPlayed);
        history.setFinalScore(calculateScore(cellsCaptured, troopsKilled, isWinner));
        history.setPlayedAt(LocalDateTime.now());
        history.setDurationMinutes(durationMinutes);

        gameHistoryRepository.save(history);
    }

    @Transactional
    public PlayerEntity getOrCreatePlayer(String playerId, String playerName) {
        PlayerEntity existing = playerRepository.findById(playerId).orElse(null);
        if (existing != null) {
            existing.setLastLogin(LocalDateTime.now());
            return playerRepository.save(existing);
        }

        PlayerEntity newPlayer = new PlayerEntity(playerId, playerName);
        newPlayer.setCreatedAt(LocalDateTime.now());
        newPlayer.setLastLogin(LocalDateTime.now());
        newPlayer.setLastPlayed(LocalDateTime.now());
        newPlayer.setTotalGames(0);
        newPlayer.setTotalWins(0);
        newPlayer.setTotalCellsCaptured(0);
        newPlayer.setTotalTroopsKilled(0);
        newPlayer.setTotalScore(0);
        return playerRepository.save(newPlayer);
    }

    public void startNewGameSession(String gameId, List<Player> players) {
        System.out.println("✅ Начата новая игровая сессия: " + gameId + " с " + players.size() + " игроками");
    }

    public void endGameSession(String gameId, Player winner, int totalTurns) {
        System.out.println("✅ Игровая сессия завершена: " + gameId +
                ", победитель: " + (winner != null ? winner.getName() : "None") +
                ", ходов: " + totalTurns);
    }

    private int calculateScore(int cellsCaptured, int troopsKilled, boolean isWinner) {
        int score = cellsCaptured * 10 + troopsKilled;
        if (isWinner) score += 100;
        return score;
    }
}