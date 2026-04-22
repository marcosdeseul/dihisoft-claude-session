package com.example.board.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthService authService;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void seed() {
        userRepository.deleteAll();
        authService.signup(new SignupRequest("alice", "pw12345"));
    }

    @Nested
    class RestApi {

        @Test
        void 로그인_성공시_200과_token_username_expiresIn을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"pw12345\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.expiresIn").value(greaterThan(0)));
        }

        @Test
        void 비밀번호가_틀리면_401과_자격증명_오류를_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value(containsString("자격")));
        }

        @Test
        void 존재하지_않는_username이면_401을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"nobody\",\"password\":\"pw12345\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        void username_누락시_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"pw12345\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("username")));
        }

        @Test
        void password_누락시_400을_반환한다() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }
    }

    @Nested
    class ProtectedAccess {

        @Test
        void 로그인에서_얻은_Bearer_토큰으로_보호경로에_접근할_수_있다() throws Exception {
            String body = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"pw12345\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode node = objectMapper.readTree(body);
            String token = node.get("token").asText();

            mockMvc.perform(get("/__probe/secured").header("Authorization", "Bearer " + token))
                    .andExpect(status().is(not(401)));
        }

        @Test
        void Bearer_토큰_없이는_보호경로가_401이다() throws Exception {
            mockMvc.perform(get("/__probe/secured"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 로그인_폼에서_심긴_AUTH_TOKEN_쿠키로_보호경로에_접근할_수_있다() throws Exception {
            jakarta.servlet.http.Cookie authCookie = mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("username", "alice")
                            .param("password", "pw12345"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn().getResponse().getCookie("AUTH_TOKEN");

            mockMvc.perform(get("/__probe/secured").cookie(authCookie))
                    .andExpect(status().is(not(401)));
        }
    }

    @Nested
    class View {

        @Test
        void GET_login은_로그인_폼을_렌더한다() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("<form")))
                    .andExpect(content().string(containsString("action=\"/login\"")))
                    .andExpect(content().string(containsString("name=\"username\"")))
                    .andExpect(content().string(containsString("name=\"password\"")))
                    .andExpect(content().string(containsString("type=\"submit\"")));
        }

        @Test
        void POST_login_정상입력시_success_리다이렉트와_AUTH_TOKEN_쿠키를_세팅한다() throws Exception {
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("username", "alice")
                            .param("password", "pw12345"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", "/login?success=true"))
                    .andExpect(cookie().exists("AUTH_TOKEN"))
                    .andExpect(cookie().httpOnly("AUTH_TOKEN", true));
        }

        @Test
        void GET_login_success시_로그인완료_메시지를_렌더한다() throws Exception {
            mockMvc.perform(get("/login").param("success", "true"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("로그인 완료")));
        }

        @Test
        void POST_login_검증_실패시_동일화면에_오류_메시지가_렌더된다() throws Exception {
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("username", "")
                            .param("password", "pw12345"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("username")));
        }

        @Test
        void POST_login_자격증명_오류시_동일화면에_오류_메시지가_렌더된다() throws Exception {
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("username", "alice")
                            .param("password", "wrong-password"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("자격")));
        }
    }
}
