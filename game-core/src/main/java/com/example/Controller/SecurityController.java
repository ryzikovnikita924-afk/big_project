package com.example.Controller;

import com.example.dto.UserInfoHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SecurityController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(UserInfoHeaders userInfoHeaders) {

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", userInfoHeaders.authenticated());

        if (userInfoHeaders.authenticated()) {
            Map<String, Object> userInfo = new HashMap<>();
            if (userInfoHeaders.user() != null) userInfo.put("user", userInfoHeaders.user());
            if (userInfoHeaders.email() != null) userInfo.put("email", userInfoHeaders.email());
            if (userInfoHeaders.preferredUsername() != null) userInfo.put("preferred_username", userInfoHeaders.preferredUsername());
            if (!userInfoHeaders.groups().isEmpty()) userInfo.put("groups", userInfoHeaders.groups());
            response.put("userInfo", userInfo);
        }

        return ResponseEntity.ok(response);
    }
}