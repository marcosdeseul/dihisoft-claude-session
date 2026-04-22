package com.example.board.security;

public class JwtTokenProvider {

    public JwtTokenProvider(String secret, long expirationMs) {
    }

    public String createToken(String username) {
        return null;
    }

    public boolean validate(String token) {
        return false;
    }

    public String parseUsername(String token) {
        return null;
    }
}
