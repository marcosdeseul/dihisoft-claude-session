package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void signup_정상입력시_User를_저장하고_password는_해시된다() {
        User saved = authService.signup(new SignupRequest("alice_srv", "pw12345"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("alice_srv");
        assertThat(saved.getPassword()).isNotEqualTo("pw12345");
        assertThat(saved.getPassword()).startsWith("$2");
    }

    @Test
    void signup_username이_null이면_IllegalArgumentException이_발생한다() {
        SignupRequest request = new SignupRequest(null, "pw12345");

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("필수");
    }

    @Test
    void signup_username이_공백만이면_trim_후_IllegalArgumentException이_발생한다() {
        SignupRequest request = new SignupRequest("   ", "pw12345");

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("필수");
    }

    @Test
    void signup_동일한_username이면_중복_메시지와_함께_IllegalArgumentException이_발생한다() {
        authService.signup(new SignupRequest("dup_srv", "pw12345"));

        assertThatThrownBy(() -> authService.signup(new SignupRequest("dup_srv", "other99")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미")
                .hasMessageContaining("dup_srv");
    }

    @Test
    void signup_username이_trim되어_저장된다() {
        User saved = authService.signup(new SignupRequest("  alice_trim  ", "pw12345"));

        assertThat(saved.getUsername()).isEqualTo("alice_trim");
    }
}
