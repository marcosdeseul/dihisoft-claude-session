package com.example.board.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Test
    void generateToken_username으로_토큰을_생성하면_subject가_username이다() {
        String token = jwtTokenProvider.generateToken("alice");
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("alice");
    }

    @Test
    void isValid_유효한_토큰은_true를_반환한다() {
        String token = jwtTokenProvider.generateToken("alice");
        assertThat(jwtTokenProvider.isValid(token)).isTrue();
    }

    @Test
    void isValid_변조된_토큰은_false를_반환한다() {
        String token = jwtTokenProvider.generateToken("alice");
        String[] parts = token.split("\\.");
        parts[2] = "invalidsignaturevalue";
        String tampered = String.join(".", parts);
        assertThat(jwtTokenProvider.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_만료된_토큰은_false를_반환한다() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-test-secret-test-secret-test-secret-test-secret-64!",
                1L);
        String token = shortLived.generateToken("alice");
        Thread.sleep(5);
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void getUsername_토큰에서_username을_추출할_수_있다() {
        String token = jwtTokenProvider.generateToken("bob");
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("bob");
    }
}
