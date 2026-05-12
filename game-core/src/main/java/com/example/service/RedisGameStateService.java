package com.example.service;

import com.example.snapshot.GameStateSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisGameStateService {

    private static final String CURRENT_GAME_KEY = "current_game_state";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisGameStateService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveCurrentGame(GameStateSnapshot snapshot) {
        try {
            snapshot.setTimestamp(System.currentTimeMillis());
            redisTemplate.opsForValue().set(CURRENT_GAME_KEY, snapshot, TTL_HOURS, TimeUnit.HOURS);
            System.out.println("✅ Текущее состояние игры сохранено в Redis");
        } catch (Exception e) {
            System.err.println("❌ Ошибка сохранения: " + e.getMessage());
        }
    }

    public GameStateSnapshot loadCurrentGame() {
        try {
            Object snapshot = redisTemplate.opsForValue().get(CURRENT_GAME_KEY);
            if (snapshot instanceof GameStateSnapshot) {
                System.out.println("✅ Состояние игры загружено из Redis");
                return (GameStateSnapshot) snapshot;
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Ошибка загрузки: " + e.getMessage());
            return null;
        }
    }

    public boolean hasCurrentGame() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CURRENT_GAME_KEY));
    }

    public void deleteCurrentGame() {
        redisTemplate.delete(CURRENT_GAME_KEY);
        System.out.println("🗑️ Состояние игры удалено из Redis");
    }
}