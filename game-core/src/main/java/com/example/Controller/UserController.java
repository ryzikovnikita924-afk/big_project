package com.example.Controller;

import com.example.entity.PlayerEntity;
import com.example.service.PlayerSyncService;
import com.example.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final PlayerSyncService playerSyncService;
    private final CurrentUserService currentUserService;

    public UserController(PlayerSyncService playerSyncService,
                          CurrentUserService currentUserService) {
        this.playerSyncService = playerSyncService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser() {
        Map<String, Object> response = new HashMap<>();

        String authId = currentUserService.getAuthId();
        if (authId == null) {
            response.put("authenticated", false);
            return response;
        }

        try {
            PlayerEntity player = playerSyncService.sync();

            response.put("authenticated", true);
            response.put("id", player.getId());
            response.put("email", player.getEmail() != null ? player.getEmail() : "");
            response.put("username", player.getName() != null ? player.getName() : "");
            response.put("name", player.getName() != null ? player.getName() : "");
            response.put("firstName", player.getFirstName() != null ? player.getFirstName() : "");
            response.put("lastName", player.getLastName() != null ? player.getLastName() : "");
            response.put("avatarUrl", player.getAvatarUrl() != null ? player.getAvatarUrl() : "");
            response.put("totalGames", player.getTotalGames());
            response.put("totalWins", player.getTotalWins());
            response.put("totalScore", player.getTotalScore());

        } catch (Exception e) {
            response.put("authenticated", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @GetMapping("/login")
    public Map<String, String> login() {
        return Map.of("redirect", "/oauth2/start");
    }

    @GetMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("redirect", "/oauth2/sign_out");
    }
}