package com.example.board.security;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.board.user.User;
import com.example.board.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "test-secret-test-secret-test-secret-test-secret-test-secret-64!";
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final String OTHER_SECRET =
            "other-secret-other-secret-other-secret-other-secret-other-secret";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
    }

    @Test
    void 유효한_Bearer_토큰이_있으면_SecurityContext에_사용자가_주입된다() throws Exception {
        String token = tokenProvider.generateToken("alice");

        mockMvc.perform(get("/__probe/secured")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("alice"));
    }

    @Test
    void 만료된_Bearer_토큰이면_401을_반환한다() throws Exception {
        String expired = new JwtTokenProvider(SECRET, -1_000L).generateToken("alice");

        mockMvc.perform(get("/__probe/secured")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(unauthenticated());
    }

    @Test
    void 서명이_조작된_Bearer_토큰이면_401을_반환한다() throws Exception {
        String forged = new JwtTokenProvider(OTHER_SECRET, ONE_HOUR_MS).generateToken("alice");

        mockMvc.perform(get("/__probe/secured")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void Bearer_prefix가_없는_헤더는_미인증으로_처리되어_401을_반환한다() throws Exception {
        String token = tokenProvider.generateToken("alice");

        mockMvc.perform(get("/__probe/secured")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void 존재하지_않는_사용자의_토큰이면_401을_반환한다() throws Exception {
        String token = tokenProvider.generateToken("ghost");

        mockMvc.perform(get("/__probe/secured")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }
}
