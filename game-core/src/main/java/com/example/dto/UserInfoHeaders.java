package com.example.dto;

import java.util.List;

public record UserInfoHeaders(
        boolean authenticated,
        String user,
        String email,
        String preferredUsername,
        List<String> groups

) {

    public static UserInfoHeaders anonymous() {
        return new UserInfoHeaders(false, null, null, null, List.of());
    }
}
