package com.example.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionService {

    private String currentGameId;
    private String playerId;
    private String aiId;
    private String difficulty;
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();

    public void createSession(String playerId, String aiId, String difficulty) {
        this.currentGameId = UUID.randomUUID().toString();
        this.playerId = playerId;
        this.aiId = aiId;
        this.difficulty = difficulty;

        GameSession session = new GameSession(currentGameId, playerId, aiId, difficulty);
        activeSessions.put(currentGameId, session);
    }

    public String getCurrentGameId() {
        return currentGameId;
    }

    public String getAiId() {
        return aiId;
    }

    public List<Map<String, Object>> getActiveSessions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (GameSession session : activeSessions.values()) {
            result.add(session.toMap());
        }
        return result;
    }

    public void clearCurrentSession() {
        if (currentGameId != null) {
            activeSessions.remove(currentGameId);
        }
        currentGameId = null;
        playerId = null;
        aiId = null;
        difficulty = null;
    }

    private static class GameSession {
        private final String gameId;
        private final String playerId;
        private final String aiId;
        private final String difficulty;
        private final long startTime;

        public GameSession(String gameId, String playerId, String aiId, String difficulty) {
            this.gameId = gameId;
            this.playerId = playerId;
            this.aiId = aiId;
            this.difficulty = difficulty;
            this.startTime = System.currentTimeMillis();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("gameId", gameId);
            map.put("difficulty", difficulty);
            map.put("startTime", startTime);
            return map;
        }
    }
}