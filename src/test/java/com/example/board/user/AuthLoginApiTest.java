package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        authService.signup(new SignupRequest("loginuser", "password123"));
    }

    // ──────────────────────────── REST API ────────────────────────────

    @Test
    void login_유효한_자격증명이면_200과_accessToken을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("loginuser", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void login_존재하지_않는_username이면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("nobody", "password123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_비밀번호가_틀리면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("loginuser", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_username이_null이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":null,\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_password가_빈값이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"loginuser\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_발급된_토큰으로_보호된_엔드포인트에_접근할_수_있다() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("loginuser", "password123"))))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("loginuser"));
    }

    @Test
    void login_토큰_없이_보호된_엔드포인트에_접근하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_변조된_토큰으로_보호된_엔드포인트에_접근하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────── UI ────────────────────────────

    @Test
    void GET_login_페이지를_반환한다() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }
}
