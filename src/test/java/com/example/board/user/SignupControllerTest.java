package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class SignupControllerTest {

  @Autowired MockMvc mockMvc;

  @Autowired UserRepository userRepository;

  @BeforeEach
  void cleanDb() {
    userRepository.deleteAll();
  }

  @Nested
  class RestApi {

    @Test
    void 회원가입_성공시_201과_id_username을_반환하고_password는_응답에_없다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"alice\",\"password\":\"pw12345\"}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.username").value("alice"))
          .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void 저장된_password는_평문이_아닌_BCrypt_해시_형식이다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"bob\",\"password\":\"pw12345\"}"))
          .andExpect(status().isCreated());

      User saved = userRepository.findByUsername("bob").orElseThrow();
      assertThat(saved.getPassword()).isNotEqualTo("pw12345");
      assertThat(saved.getPassword()).startsWith("$2");
    }

    @Test
    void username이_null이면_400을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"password\":\"pw12345\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void username이_빈문자열이면_400을_반환하고_필드명을_포함한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"\",\"password\":\"pw12345\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(containsString("username")));
    }

    @Test
    void username이_공백만이면_400을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"   \",\"password\":\"pw12345\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void username이_2자면_400을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"ab\",\"password\":\"pw12345\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void username이_21자면_400을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"a23456789012345678901\",\"password\":\"pw12345\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void password가_null이면_400을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"alice\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(containsString("password")));
    }

    @Test
    void password가_5자면_400을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"alice\",\"password\":\"12345\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void 동일_username으로_재가입하면_400과_중복_메시지를_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"dup\",\"password\":\"pw12345\"}"))
          .andExpect(status().isCreated());

      mockMvc
          .perform(
              post("/api/auth/signup")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"dup\",\"password\":\"pw99999\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(containsString("이미")));
    }
  }

  @Nested
  class Spa {

    @Test
    void GET_signup은_React_SPA_index_html로_forward된다() throws Exception {
      mockMvc
          .perform(get("/signup"))
          .andExpect(status().isOk())
          .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void POST_signup_form_encoded는_form_핸들러가_없어_4xx를_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/signup")
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                  .param("username", "alice")
                  .param("password", "pw12345"))
          .andExpect(status().is4xxClientError());
    }
  }
}
