package com.example.Controller;

import com.example.entity.PlayerEntity;
import com.example.service.PlayerSyncService;
import com.example.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

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

        String authId = currentUserService.getAuthId();
        if (authId == null) {
            return Map.of("authenticated", false);
        }

        PlayerEntity player = playerSyncService.sync();

        return Map.of(
                "authenticated", true,
                "id", player.getId(),
                "email", player.getEmail(),
                "username", player.getName(),
                "firstName", player.getFirstName(),
                "lastName", player.getLastName(),
                "avatarUrl", player.getAvatarUrl(),
                "totalGames", player.getTotalGames(),
                "totalWins", player.getTotalWins(),
                "totalScore", player.getTotalScore()
        );
    }

    @GetMapping("/login")
    public Map<String, String> login() {
        return Map.of("redirect", "/oauth2/start"); // отдаёт oauth2-proxy
    }

    @GetMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("redirect", "/oauth2/sign_out");
    }
}