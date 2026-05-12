package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_history")
public class GameHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private PlayerEntity player;

    @Column(name = "game_id")
    private String gameId;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "player_color")
    private String playerColor;

    @Column(name = "is_winner")
    private boolean isWinner;

    @Column(name = "cells_captured")
    private int cellsCaptured;

    @Column(name = "troops_killed")
    private int troopsKilled;

    @Column(name = "turns_played")
    private int turnsPlayed;

    @Column(name = "final_score")
    private int finalScore;

    @Column(name = "played_at")
    private LocalDateTime playedAt;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    public GameHistoryEntity() {}

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PlayerEntity getPlayer() { return player; }
    public void setPlayer(PlayerEntity player) { this.player = player; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getPlayerColor() { return playerColor; }
    public void setPlayerColor(String playerColor) { this.playerColor = playerColor; }

    public boolean isWinner() { return isWinner; }
    public void setWinner(boolean winner) { isWinner = winner; }

    public int getCellsCaptured() { return cellsCaptured; }
    public void setCellsCaptured(int cellsCaptured) { this.cellsCaptured = cellsCaptured; }

    public int getTroopsKilled() { return troopsKilled; }
    public void setTroopsKilled(int troopsKilled) { this.troopsKilled = troopsKilled; }

    public int getTurnsPlayed() { return turnsPlayed; }
    public void setTurnsPlayed(int turnsPlayed) { this.turnsPlayed = turnsPlayed; }

    public int getFinalScore() { return finalScore; }
    public void setFinalScore(int finalScore) { this.finalScore = finalScore; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}