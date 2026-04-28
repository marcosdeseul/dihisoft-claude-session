package com.example.board.user;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.board.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(LoginControllerTest.ProbeController.class)
class LoginControllerTest {

  @Autowired MockMvc mockMvc;

  @Autowired UserRepository userRepository;

  @Autowired PasswordEncoder passwordEncoder;

  @Autowired JwtTokenProvider jwtTokenProvider;

  @Value("${app.jwt.secret}")
  String jwtSecret;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
  }

  @RestController
  static class ProbeController {
    @GetMapping("/api/probe/me")
    public Map<String, Object> me(Authentication authentication) {
      return Map.of("username", authentication.getName());
    }
  }

  @Nested
  class RestApi {

    @Test
    void 로그인_성공시_200과_accessToken_tokenType_expiresIn을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"alice\",\"password\":\"pw12345\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").isString())
          .andExpect(jsonPath("$.tokenType").value("Bearer"))
          .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void 잘못된_password면_401과_ErrorResponse_본문을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"alice\",\"password\":\"wrong-password\"}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.error").value("Unauthorized"))
          .andExpect(jsonPath("$.message").exists())
          .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void 존재하지_않는_username이면_401을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"nobody\",\"password\":\"pw12345\"}"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void username이_null이면_400을_반환하고_필드명을_포함한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"password\":\"pw12345\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(containsString("username")));
    }

    @Test
    void password가_null이면_400을_반환하고_필드명을_포함한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"alice\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(containsString("password")));
    }

    @Test
    void username이_빈문자열이면_400을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"\",\"password\":\"pw12345\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void password가_빈문자열이면_400을_반환한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"alice\",\"password\":\"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void username이_공백만이면_400을_반환하고_필드명을_포함한다() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"   \",\"password\":\"pw12345\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(containsString("username")));
    }
  }

  @Nested
  class JwtFilter {

    @Test
    void 유효한_Bearer_토큰으로_보호엔드포인트_호출시_200과_username이_반환된다() throws Exception {
      String token = jwtTokenProvider.generateToken("alice");
      mockMvc
          .perform(get("/api/probe/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void 토큰없이_보호엔드포인트_호출시_401이_반환된다() throws Exception {
      mockMvc.perform(get("/api/probe/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void Bearer_접두어_없이_토큰만_보내면_401이_반환된다() throws Exception {
      String token = jwtTokenProvider.generateToken("alice");
      mockMvc
          .perform(get("/api/probe/me").header("Authorization", token))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void 변조된_토큰으로_호출시_401이_반환된다() throws Exception {
      String token = jwtTokenProvider.generateToken("alice");
      String tampered = token.substring(0, token.length() - 4) + "XXXX";
      mockMvc
          .perform(get("/api/probe/me").header("Authorization", "Bearer " + tampered))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void 만료된_토큰으로_호출시_401이_반환된다() throws Exception {
      SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
      Date past = new Date(System.currentTimeMillis() - 10_000);
      String expiredToken =
          Jwts.builder()
              .subject("alice")
              .issuedAt(new Date(past.getTime() - 1000))
              .expiration(past)
              .signWith(key, Jwts.SIG.HS256)
              .compact();
      mockMvc
          .perform(get("/api/probe/me").header("Authorization", "Bearer " + expiredToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void 서명은_유효하지만_sub_claim이_없는_토큰으로_호출시_401이_반환된다() throws Exception {
      SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
      String noSubjectToken =
          Jwts.builder()
              .issuedAt(new Date())
              .expiration(new Date(System.currentTimeMillis() + 60_000))
              .signWith(key, Jwts.SIG.HS256)
              .compact();
      mockMvc
          .perform(get("/api/probe/me").header("Authorization", "Bearer " + noSubjectToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_토큰이지만_사용자가_DB에서_삭제됐으면_401이_반환된다() throws Exception {
      String token = jwtTokenProvider.generateToken("alice");
      userRepository.deleteAll();
      mockMvc
          .perform(get("/api/probe/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void 다른_서명키로_만든_토큰으로_호출시_401이_반환된다() throws Exception {
      String otherSecret = "different-secret-different-secret-different-secret-64bytes!!!!!";
      SecretKey wrongKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));
      String foreignToken =
          Jwts.builder()
              .subject("alice")
              .issuedAt(new Date())
              .expiration(new Date(System.currentTimeMillis() + 60_000))
              .signWith(wrongKey, Jwts.SIG.HS256)
              .compact();
      mockMvc
          .perform(get("/api/probe/me").header("Authorization", "Bearer " + foreignToken))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  class View {

    @Test
    void GET_login은_로그인폼을_렌더한다() throws Exception {
      mockMvc
          .perform(get("/login"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("<form")))
          .andExpect(content().string(containsString("action=\"/login\"")))
          .andExpect(content().string(containsString("name=\"username\"")))
          .andExpect(content().string(containsString("name=\"password\"")))
          .andExpect(content().string(containsString("type=\"submit\"")));
    }

    @Test
    void POST_login_정상입력시_302_redirect와_HttpOnly_accessToken_쿠키가_세팅된다() throws Exception {
      mockMvc
          .perform(
              post("/login")
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                  .param("username", "alice")
                  .param("password", "pw12345"))
          .andExpect(status().is3xxRedirection())
          .andExpect(header().string("Location", "/login?success=true"))
          .andExpect(cookie().exists("accessToken"))
          .andExpect(cookie().httpOnly("accessToken", true));
    }

    @Test
    void POST_login_잘못된_password면_동일화면에_오류메시지가_렌더된다() throws Exception {
      mockMvc
          .perform(
              post("/login")
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                  .param("username", "alice")
                  .param("password", "wrong-password"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("<form")))
          .andExpect(content().string(containsString("일치")));
    }

    @Test
    void POST_login_존재하지_않는_username이면_동일화면에_오류메시지가_렌더된다() throws Exception {
      mockMvc
          .perform(
              post("/login")
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                  .param("username", "nobody")
                  .param("password", "pw12345"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("<form")));
    }

    @Test
    void POST_login_username_빈문자열이면_동일화면에_검증_오류가_렌더된다() throws Exception {
      mockMvc
          .perform(
              post("/login")
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                  .param("username", "")
                  .param("password", "pw12345"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("username")));
    }

    @Test
    void GET_login_success_파라미터시_성공메시지를_렌더한다() throws Exception {
      mockMvc
          .perform(get("/login").param("success", "true"))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("로그인 성공")));
    }
  }
}
