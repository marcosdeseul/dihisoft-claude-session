package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Test
    void createToken_후_getUsername으로_동일한_username을_복원한다() {
        String token = jwtTokenProvider.createToken("alice");

        assertThat(jwtTokenProvider.validate(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("alice");
    }

    @Test
    void validate는_변조된_서명을_거부한다() {
        String token = jwtTokenProvider.createToken("alice");
        String tampered = token.substring(0, token.length() - 2) + "XX";

        assertThat(jwtTokenProvider.validate(tampered)).isFalse();
    }

    @Test
    void validate는_null이나_빈_토큰을_거부한다() {
        assertThat(jwtTokenProvider.validate(null)).isFalse();
        assertThat(jwtTokenProvider.validate("")).isFalse();
        assertThat(jwtTokenProvider.validate("not-a-jwt")).isFalse();
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "app.jwt.expiration-ms=1")
    @org.springframework.test.annotation.DirtiesContext
    static class Expiration {

        @Autowired
        JwtTokenProvider shortLivedProvider;

        @Test
        void validate는_만료된_토큰을_거부한다() throws InterruptedException {
            String token = shortLivedProvider.createToken("alice");
            Thread.sleep(50);

            assertThat(shortLivedProvider.validate(token)).isFalse();
        }
    }
}
