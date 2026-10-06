package com.taskflow.auth;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String username,
        Set<String> roles) {

    public AuthResponse(String accessToken, String refreshToken, String username, Set<String> roles) {
        this(accessToken, refreshToken, "Bearer", username, roles);
    }
}
