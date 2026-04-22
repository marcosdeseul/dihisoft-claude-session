package com.example.board.user;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
