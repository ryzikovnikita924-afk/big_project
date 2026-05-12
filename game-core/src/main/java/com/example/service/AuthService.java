package com.example.service;

import com.example.entity.PlayerEntity;
import com.example.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService {

    private final PlayerRepository playerRepository;

    public AuthService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public PlayerEntity authenticateOrCreateUser(Map<String, Object> userInfo) {
        String email = (String) userInfo.get("email");
        String username = (String) userInfo.get("preferred_username");

        if (username == null) {
            username = email.split("@")[0];
        }

        String firstName = (String) userInfo.get("given_name");
        String lastName = (String) userInfo.get("family_name");
        String authId = (String) userInfo.get("sub");

        // Ищем пользователя по email или authId
        PlayerEntity player = playerRepository.findByEmail(email).orElse(null);

        if (player == null && authId != null) {
            player = playerRepository.findByAuthId(authId).orElse(null);
        }

        if (player == null) {
            // Создаем нового игрока
            player = new PlayerEntity(authId, username);
            player.setEmail(email);
            player.setFirstName(firstName);
            player.setLastName(lastName);

            // Устанавливаем avatar (если есть)
            if (userInfo.containsKey("picture")) {
                player.setAvatarUrl((String) userInfo.get("picture"));
            }

            player.setAuthId(authId);
            player.setCreatedAt(LocalDateTime.now());
            player.setLastLogin(LocalDateTime.now());
            player.setTotalGames(0);
            player.setTotalWins(0);
            player.setTotalCellsCaptured(0);
            player.setTotalTroopsKilled(0);
            player.setTotalScore(0);

            playerRepository.save(player);
            System.out.println("✅ Создан новый игрок: " + username);
        } else {
            // Обновляем информацию о последнем входе
            player.setLastLogin(LocalDateTime.now());
            playerRepository.save(player);
            System.out.println("✅ Игрок авторизован: " + player.getName());
        }

        return player;
    }

    @Transactional
    public PlayerEntity getOrCreatePlayer(String playerId, String playerName) {
        return playerRepository.findById(playerId).orElseGet(() -> {
            PlayerEntity newPlayer = new PlayerEntity(playerId, playerName);
            newPlayer.setCreatedAt(LocalDateTime.now());
            newPlayer.setLastLogin(LocalDateTime.now());
            return playerRepository.save(newPlayer);
        });
    }
}