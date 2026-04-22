package com.example.board.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-test-secret-test-secret-test-secret-test-secret-64!";
    private static final long ONE_HOUR_MS = 3_600_000L;

    @Test
    void 토큰_생성시_서명_만료_subject_클레임이_올바르다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, ONE_HOUR_MS);

        String token = provider.generateToken("alice");

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(ONE_HOUR_MS);
    }

    @Test
    void 유효한_토큰은_validateToken이_true를_반환한다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, ONE_HOUR_MS);
        String token = provider.generateToken("alice");

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void 만료된_토큰은_validateToken이_false를_반환한다() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1_000L);
        String expired = expiredProvider.generateToken("alice");

        JwtTokenProvider provider = new JwtTokenProvider(SECRET, ONE_HOUR_MS);
        assertThat(provider.validateToken(expired)).isFalse();
    }

    @Test
    void 서명이_다른_토큰은_validateToken이_false를_반환한다() {
        String otherSecret =
                "other-secret-other-secret-other-secret-other-secret-other-secret";
        String forged = new JwtTokenProvider(otherSecret, ONE_HOUR_MS).generateToken("alice");

        JwtTokenProvider provider = new JwtTokenProvider(SECRET, ONE_HOUR_MS);
        assertThat(provider.validateToken(forged)).isFalse();
    }

    @Test
    void 형식이_잘못된_토큰은_validateToken이_false를_반환한다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, ONE_HOUR_MS);

        assertThat(provider.validateToken("not-a-jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken(null)).isFalse();
    }

    @Test
    void 유효_토큰에서_getUsername이_sub를_반환한다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, ONE_HOUR_MS);
        String token = provider.generateToken("bob");

        assertThat(provider.getUsername(token)).isEqualTo("bob");
    }
}
