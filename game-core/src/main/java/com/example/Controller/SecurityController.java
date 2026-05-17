package com.example.Controller;

import com.example.dto.UniversalResponse;
import com.example.dto.auth.AuthStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class SecurityController {

    @GetMapping("/me")
    public ResponseEntity<UniversalResponse<AuthStateResponse>> currentUser(
            Authentication authentication,
            CsrfToken csrfToken
    ) {
        csrfToken.getToken();

        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal;

        Map<String, Object> userInfo = Collections.emptyMap();
        if (authenticated) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                userInfo = oidcUser.getClaims();
            } else {
                userInfo = ((OAuth2AuthenticatedPrincipal) principal).getAttributes();
            }
        }

        AuthStateResponse response = authenticated
                ? new AuthStateResponse(true, userInfo)
                : new AuthStateResponse(false, userInfo);
        HttpStatus status = authenticated ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(new UniversalResponse<>(response));
    }
}