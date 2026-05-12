package com.example.snapshot;

import java.io.Serializable;
import java.util.*;

public class GameStateSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, CellSnapshot> cells;
    private Map<String, PlayerSnapshot> players;
    private String currentTurnPlayerId;
    private int turnNumber;
    private String gameState;
    private String winnerId;
    private String gameId;
    private long timestamp;

    public GameStateSnapshot() {}

    public Map<String, CellSnapshot> getCells() { return cells; }
    public void setCells(Map<String, CellSnapshot> cells) { this.cells = cells; }

    public Map<String, PlayerSnapshot> getPlayers() { return players; }
    public void setPlayers(Map<String, PlayerSnapshot> players) { this.players = players; }

    public String getCurrentTurnPlayerId() { return currentTurnPlayerId; }
    public void setCurrentTurnPlayerId(String currentTurnPlayerId) { this.currentTurnPlayerId = currentTurnPlayerId; }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public String getGameState() { return gameState; }
    public void setGameState(String gameState) { this.gameState = gameState; }

    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}