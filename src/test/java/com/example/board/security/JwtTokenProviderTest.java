package com.example.board.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-test-secret-test-secret-test-secret-test-secret-64!";
    private static final long EXPIRATION_MS = 60_000L;

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, EXPIRATION_MS);

    @Test
    void createToken_은_HS256_JWT_3세그먼트_문자열을_반환한다() {
        String token = provider.createToken("alice");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void validate_는_방금_발급한_토큰에_대해_true를_반환한다() {
        String token = provider.createToken("alice");

        assertThat(provider.validate(token)).isTrue();
    }

    @Test
    void parseUsername_은_토큰의_sub_클레임에서_username을_꺼낸다() {
        String token = provider.createToken("alice");

        assertThat(provider.parseUsername(token)).isEqualTo("alice");
    }

    @Test
    void 만료된_토큰에_대해_validate는_false를_반환한다() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 10L);
        String token = shortLived.createToken("alice");

        TimeUnit.MILLISECONDS.sleep(200);

        assertThat(shortLived.validate(token)).isFalse();
    }

    @Test
    void 서명이_변조된_토큰에_대해_validate는_false를_반환한다() {
        String token = provider.createToken("alice");
        String tampered = token.substring(0, token.length() - 2) + "XX";

        assertThat(provider.validate(tampered)).isFalse();
    }

    @Test
    void 다른_secret으로_발급된_토큰에_대해_validate는_false를_반환한다() {
        JwtTokenProvider other = new JwtTokenProvider(
                "another-secret-another-secret-another-secret-another-secret-64!",
                EXPIRATION_MS);
        String token = other.createToken("alice");

        assertThat(provider.validate(token)).isFalse();
    }

    @Test
    void null이나_빈문자열_토큰에_대해_validate는_false를_반환한다() {
        assertThat(provider.validate(null)).isFalse();
        assertThat(provider.validate("")).isFalse();
        assertThat(provider.validate("   ")).isFalse();
        assertThat(provider.validate("not.a.jwt.token")).isFalse();
    }
}
