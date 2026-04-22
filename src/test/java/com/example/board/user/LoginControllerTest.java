package com.example.board.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Nested
    class RestApi {

        @BeforeEach
        void createUser() {
            userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
        }

        @Test
        void 로그인_성공시_200과_token을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"pw12345\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.token", startsWith("eyJ")));
        }

        @Test
        void 존재하지않는_username이면_401을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"nobody\",\"password\":\"pw12345\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 비밀번호가_틀리면_401을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"wrongpw\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void username이_null이면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"pw12345\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void username이_빈문자열이면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"\",\"password\":\"pw12345\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void password가_null이면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void password가_빈문자열이면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class JwtFilter {

        private String token;

        @BeforeEach
        void createUserAndGetToken() throws Exception {
            userRepository.save(new User("filter_user", passwordEncoder.encode("pw12345")));
            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"filter_user\",\"password\":\"pw12345\"}"))
                    .andReturn().getResponse().getContentAsString();
            token = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        }

        @Test
        void 발급된_토큰으로_보호된_엔드포인트에_접근하면_200을_반환한다() throws Exception {
            mockMvc.perform(get("/__probe/secured")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        void 토큰_없이_보호된_엔드포인트에_접근하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/__probe/secured"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 변조된_토큰으로_접근하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/__probe/secured")
                            .header("Authorization", "Bearer " + token + "tampered"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class View {

        @Test
        void GET_login은_로그인_폼을_렌더한다() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(containsString("action=\"/login\"")))
                    .andExpect(content().string(containsString("id=\"username\"")))
                    .andExpect(content().string(containsString("id=\"password\"")));
        }

        @Test
        void POST_login_성공시_success_파라미터로_리다이렉트된다() throws Exception {
            userRepository.save(new User("view_user", passwordEncoder.encode("pw12345")));
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("username", "view_user")
                            .param("password", "pw12345"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?success=true"));
        }

        @Test
        void POST_login_실패시_동일화면에_오류_메시지가_렌더된다() throws Exception {
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("username", "nobody")
                            .param("password", "wrongpw"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("id=\"username\"")));
        }
    }
}
