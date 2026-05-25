package com.example.service;

import com.example.snapshot.GameStateSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisGameStateService {

    private static final String GAME_STATE_PREFIX = "game:state:user:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisGameStateService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        System.out.println("🔴 RedisGameStateService инициализирован");
        testConnection();
    }

    private String getKey(String userId) {
        return GAME_STATE_PREFIX + userId;
    }

    public void saveCurrentGame(String userId, GameStateSnapshot snapshot) {
        System.out.println("\n🔴 СОХРАНЕНИЕ В REDIS для пользователя: " + userId);
        String key = getKey(userId);
        System.out.println("🔴 Ключ: " + key);

        if (snapshot == null) {
            System.err.println("❌ Snapshot is NULL");
            return;
        }

        try {
            System.out.println("🔴 Game ID: " + snapshot.getGameId());
            System.out.println("🔴 Turn: " + snapshot.getTurnNumber());

            snapshot.setTimestamp(System.currentTimeMillis());

            redisTemplate.opsForValue().set(key, snapshot, TTL_HOURS, TimeUnit.HOURS);
            System.out.println("✅ REDIS: данные сохранены для пользователя " + userId);
        } catch (Exception e) {
            System.err.println("❌ Ошибка сохранения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public GameStateSnapshot loadCurrentGame(String userId) {
        System.out.println("\n🔴 ЗАГРУЗКА ИЗ REDIS для пользователя: " + userId);
        String key = getKey(userId);
        System.out.println("🔴 Ключ: " + key);

        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!Boolean.TRUE.equals(hasKey)) {
                System.out.println("🔴 Ключ не найден для пользователя " + userId);
                return null;
            }

            Object snapshot = redisTemplate.opsForValue().get(key);

            if (snapshot instanceof GameStateSnapshot) {
                GameStateSnapshot loaded = (GameStateSnapshot) snapshot;
                System.out.println("✅ REDIS: данные загружены для пользователя " + userId);
                System.out.println("   - Game ID: " + loaded.getGameId());
                System.out.println("   - Turn: " + loaded.getTurnNumber());
                return loaded;
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Ошибка загрузки: " + e.getMessage());
            return null;
        }
    }

    public boolean hasCurrentGame(String userId) {
        try {
            String key = getKey(userId);
            Boolean hasKey = redisTemplate.hasKey(key);
            boolean exists = Boolean.TRUE.equals(hasKey);
            System.out.println("🔴 Проверка для " + userId + ": " + exists);
            return exists;
        } catch (Exception e) {
            System.err.println("❌ Ошибка проверки: " + e.getMessage());
            return false;
        }
    }

    public void deleteCurrentGame(String userId) {
        try {
            String key = getKey(userId);
            redisTemplate.delete(key);
            System.out.println("🗑️ Ключ удален для пользователя: " + userId);
        } catch (Exception e) {
            System.err.println("❌ Ошибка удаления: " + e.getMessage());
        }
    }

    private void testConnection() {
        try {
            String testKey = "test:connection:" + System.currentTimeMillis();
            String testValue = "test_value";

            redisTemplate.opsForValue().set(testKey, testValue, 10, TimeUnit.SECONDS);
            String retrieved = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);

            if (testValue.equals(retrieved)) {
                System.out.println("✅ Redis подключен и работает");
            } else {
                System.err.println("❌ Redis НЕ РАБОТАЕТ");
            }
        } catch (Exception e) {
            System.err.println("❌ Нет подключения к Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }
}