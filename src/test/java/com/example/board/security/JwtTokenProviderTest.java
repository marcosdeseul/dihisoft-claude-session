package com.example.board.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET_A =
            "test-secret-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String SECRET_B =
            "test-secret-BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

    @Test
    void generate_한_토큰을_validate로_검증하고_username을_추출할_수_있다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET_A, 60_000L);

        String token = provider.generate("alice");

        assertThat(token).isNotBlank();
        assertThat(provider.validate(token)).isTrue();
        assertThat(provider.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void 만료된_토큰은_validate가_false를_반환한다() throws InterruptedException {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET_A, 1L);
        String token = provider.generate("alice");

        Thread.sleep(50L);

        assertThat(provider.validate(token)).isFalse();
    }

    @Test
    void 서명이_다른_secret으로_만든_토큰은_validate가_false를_반환한다() {
        JwtTokenProvider signer = new JwtTokenProvider(SECRET_A, 60_000L);
        JwtTokenProvider verifier = new JwtTokenProvider(SECRET_B, 60_000L);

        String token = signer.generate("alice");

        assertThat(verifier.validate(token)).isFalse();
    }

    @Test
    void malformed_토큰은_validate가_false를_반환한다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET_A, 60_000L);

        assertThat(provider.validate("not-a-jwt")).isFalse();
        assertThat(provider.validate("aaa.bbb.ccc")).isFalse();
    }

    @Test
    void null_또는_빈_토큰은_validate가_false를_반환한다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET_A, 60_000L);

        assertThat(provider.validate(null)).isFalse();
        assertThat(provider.validate("")).isFalse();
        assertThat(provider.validate("   ")).isFalse();
    }

    @Test
    void getExpirationMs는_생성자에_주입된_값을_반환한다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET_A, 3_600_000L);

        assertThat(provider.getExpirationMs()).isEqualTo(3_600_000L);
    }
}
