package com.example.board.user;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    public static LoginResponse bearer(String accessToken, long expirationMs) {
        return new LoginResponse(accessToken, "Bearer", expirationMs / 1000L);
    }
}
