package com.example.board.security;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public String generateToken(String username) {
        throw new UnsupportedOperationException("JwtTokenProvider.generateToken 미구현 (RED)");
    }

    public Optional<String> resolveUsername(String token) {
        throw new UnsupportedOperationException("JwtTokenProvider.resolveUsername 미구현 (RED)");
    }
}
