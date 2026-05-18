package com.example.Controller;

import com.example.dto.UniversalResponse;
import com.example.dto.auth.AuthStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class SecurityController {

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> currentUser(
            Authentication authentication,
            CsrfToken csrfToken
    ) {
        // Проверяем аутентификацию
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String);

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", authenticated);

        if (authenticated) {
            Map<String, Object> userInfo = new HashMap<>();

            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                OAuth2AuthenticatedPrincipal principal = oauthToken.getPrincipal();

                // Извлекаем нужные поля
                userInfo.put("name", principal.getAttribute("name"));
                userInfo.put("email", principal.getAttribute("email"));
                userInfo.put("preferred_username", principal.getAttribute("preferred_username"));
                userInfo.put("sub", principal.getAttribute("sub"));

                response.put("user", userInfo);
                response.put("provider", oauthToken.getAuthorizedClientRegistrationId());
            }

            if (csrfToken != null) {
                response.put("csrfToken", csrfToken.getToken());
            }

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("authenticated", false, "error", "Not authenticated"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Spring Security обрабатывает logout через LogoutFilter
        return ResponseEntity.ok(Map.of("success", true));
    }
}