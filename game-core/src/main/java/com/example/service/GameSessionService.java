package com.example.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionService {

    private final Map<String, GameSession> userSessions = new ConcurrentHashMap<>();

    public void createSession(String userId, String playerId, String aiId, String difficulty) {
        String gameId = UUID.randomUUID().toString();
        GameSession session = new GameSession(gameId, playerId, aiId, difficulty);
        userSessions.put(userId, session);
        System.out.println("✅ Сессия создана для пользователя " + userId + ", Game ID: " + gameId);
    }

    public GameSession getSession(String userId) {
        return userSessions.get(userId);
    }

    public String getCurrentGameId(String userId) {
        GameSession session = userSessions.get(userId);
        return session != null ? session.getGameId() : null;
    }

    public String getAiId(String userId) {
        GameSession session = userSessions.get(userId);
        return session != null ? session.getAiId() : null;
    }

    public List<Map<String, Object>> getActiveSessions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, GameSession> entry : userSessions.entrySet()) {
            result.add(entry.getValue().toMap(entry.getKey()));
        }
        return result;
    }

    public void clearSession(String userId) {
        userSessions.remove(userId);
        System.out.println("🗑️ Сессия удалена для пользователя: " + userId);
    }

    public boolean hasSession(String userId) {
        return userSessions.containsKey(userId);
    }

    public static class GameSession {
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

        public String getGameId() { return gameId; }
        public String getPlayerId() { return playerId; }
        public String getAiId() { return aiId; }
        public String getDifficulty() { return difficulty; }
        public long getStartTime() { return startTime; }

        public Map<String, Object> toMap(String userId) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            map.put("gameId", gameId);
            map.put("difficulty", difficulty);
            map.put("startTime", startTime);
            return map;
        }
    }
}