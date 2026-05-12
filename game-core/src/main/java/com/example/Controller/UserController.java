package com.example.Controller;

import com.example.entity.PlayerEntity;
import com.example.service.AuthService;
import com.example.service.StatisticsService;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;
    private final StatisticsService statisticsService;

    public UserController(AuthService authService, StatisticsService statisticsService) {
        this.authService = authService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(HttpServletRequest request) {
        Map<String, Object> userInfo = getUserInfoFromHeaders(request);

        PlayerEntity player = authService.authenticateOrCreateUser(userInfo);

        Map<String, Object> response = new HashMap<>();
        response.put("id", player.getId());
        response.put("email", player.getEmail());
        response.put("username", player.getName());  // В PlayerEntity используется getName()
        response.put("firstName", player.getFirstName());
        response.put("lastName", player.getLastName());
        response.put("avatarUrl", player.getAvatarUrl());
        response.put("totalGames", player.getTotalGames());
        response.put("totalWins", player.getTotalWins());
        response.put("totalScore", player.getTotalScore());

        return response;
    }

    @GetMapping("/stats")
    public Map<String, Object> getUserStats(HttpServletRequest request) {
        Map<String, Object> userInfo = getUserInfoFromHeaders(request);
        String email = (String) userInfo.get("email");

        // Ищем игрока по email
        PlayerEntity player = statisticsService.getPlayerByEmail(email);

        if (player == null) {
            return Map.of("error", "Player not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", player.getId());
        response.put("name", player.getName());
        response.put("email", player.getEmail());
        response.put("totalGames", player.getTotalGames());
        response.put("totalWins", player.getTotalWins());
        response.put("totalCellsCaptured", player.getTotalCellsCaptured());
        response.put("totalTroopsKilled", player.getTotalTroopsKilled());
        response.put("totalScore", player.getTotalScore());
        response.put("createdAt", player.getCreatedAt());
        response.put("lastLogin", player.getLastLogin());

        return response;
    }

    @GetMapping("/stats/{playerId}")
    public Map<String, Object> getPlayerStats(@PathVariable String playerId) {
        PlayerEntity player = statisticsService.getPlayerById(playerId);

        if (player == null) {
            return Map.of("error", "Player not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", player.getId());
        response.put("name", player.getName());
        response.put("email", player.getEmail());
        response.put("totalGames", player.getTotalGames());
        response.put("totalWins", player.getTotalWins());
        response.put("totalCellsCaptured", player.getTotalCellsCaptured());
        response.put("totalTroopsKilled", player.getTotalTroopsKilled());
        response.put("totalScore", player.getTotalScore());

        return response;
    }

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        List<PlayerEntity> leaderboard = statisticsService.getLeaderboard();

        List<Map<String, Object>> result = new ArrayList<>();
        for (PlayerEntity player : leaderboard) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", player.getId());
            entry.put("name", player.getName());
            entry.put("totalWins", player.getTotalWins());
            entry.put("totalGames", player.getTotalGames());
            entry.put("totalScore", player.getTotalScore());
            entry.put("avatarUrl", player.getAvatarUrl());
            result.add(entry);
        }

        return result;
    }

    @GetMapping("/history")
    public List<Map<String, Object>> getPlayerHistory(HttpServletRequest request) {
        Map<String, Object> userInfo = getUserInfoFromHeaders(request);
        String email = (String) userInfo.get("email");

        PlayerEntity player = statisticsService.getPlayerByEmail(email);
        if (player == null) {
            return List.of();
        }

        return statisticsService.getPlayerHistory(player.getId());
    }

    private Map<String, Object> getUserInfoFromHeaders(HttpServletRequest request) {
        Map<String, Object> userInfo = new HashMap<>();

        // OAuth2-Proxy добавляет эти заголовки
        userInfo.put("sub", request.getHeader("X-Forwarded-User"));
        userInfo.put("email", request.getHeader("X-Forwarded-Email"));
        userInfo.put("preferred_username", request.getHeader("X-Forwarded-Preferred-Username"));
        userInfo.put("given_name", request.getHeader("X-Forwarded-Given-Name"));
        userInfo.put("family_name", request.getHeader("X-Forwarded-Family-Name"));
        userInfo.put("picture", request.getHeader("X-Forwarded-Picture"));

        return userInfo;
    }
}