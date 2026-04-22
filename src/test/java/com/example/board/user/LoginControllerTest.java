package com.example.board.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.board.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginControllerTest {

    private static final String USERNAME = "loginuser";
    private static final String PASSWORD = "pw12345";
    private static final String ALT_SECRET =
            "alt-secret-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthService authService;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void cleanAndSeed() {
        userRepository.deleteAll();
        authService.signup(new SignupRequest(USERNAME, PASSWORD));
    }

    @Nested
    class RestApi {

        @Test
        void 로그인_성공시_200과_accessToken_tokenType_expiresIn을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.accessToken").value(startsWith("ey")))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber());
        }

        @Test
        void 존재하지_않는_username이면_401과_ErrorResponse를_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"ghost\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void 잘못된_password면_401을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + USERNAME + "\",\"password\":\"wrong-pw\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        void username이_null이면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("username")));
        }

        @Test
        void password가_null이면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + USERNAME + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        void username이_빈문자열이면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void password가_빈문자열이면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + USERNAME + "\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void password는_응답에_포함되지_않는다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
    }

    @Nested
    class JwtFilter {

        @Test
        void 유효한_Bearer_토큰이면_보호_엔드포인트에_접근할_수_있다() throws Exception {
            String token = jwtTokenProvider.generate(USERNAME);

            mockMvc.perform(get("/__probe/secured")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        void Authorization_헤더가_없으면_401이다() throws Exception {
            mockMvc.perform(get("/__probe/secured"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void Bearer_prefix가_아니면_401이다() throws Exception {
            String token = jwtTokenProvider.generate(USERNAME);

            mockMvc.perform(get("/__probe/secured")
                            .header("Authorization", "Token " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 서명이_틀린_토큰이면_401이다() throws Exception {
            String tampered = new JwtTokenProvider(ALT_SECRET, 60_000L).generate(USERNAME);

            mockMvc.perform(get("/__probe/secured")
                            .header("Authorization", "Bearer " + tampered))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 만료된_토큰이면_401이다() throws Exception {
            String expired = new JwtTokenProvider(
                    "test-secret-test-secret-test-secret-test-secret-test-secret-64!",
                    1L)
                    .generate(USERNAME);
            Thread.sleep(50L);

            mockMvc.perform(get("/__probe/secured")
                            .header("Authorization", "Bearer " + expired))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void malformed_토큰이면_401이다() throws Exception {
            mockMvc.perform(get("/__probe/secured")
                            .header("Authorization", "Bearer not-a-valid-jwt"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
