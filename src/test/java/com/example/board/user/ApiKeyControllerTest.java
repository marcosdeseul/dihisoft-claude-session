package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.board.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
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
class ApiKeyControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ApiKeyService apiKeyService;
  @Autowired ApiKeyRepository apiKeyRepository;
  @Autowired UserRepository userRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired JwtTokenProvider jwtTokenProvider;

  String aliceToken;
  String bobToken;

  @BeforeEach
  void setUp() {
    apiKeyRepository.deleteAll();
    userRepository.deleteAll();
    userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
    userRepository.save(new User("bob", passwordEncoder.encode("pw12345")));
    aliceToken = "Bearer " + jwtTokenProvider.generateToken("alice");
    bobToken = "Bearer " + jwtTokenProvider.generateToken("bob");
  }

  @AfterEach
  void tearDown() {
    apiKeyRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  class 발급 {

    @Test
    void JWT_없으면_401() throws Exception {
      mockMvc
          .perform(
              post("/api/me/api-keys")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"label\":\"k\"}"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void 정상_label이면_201_평문_key_35자_bk_접두를_1회_노출한다() throws Exception {
      mockMvc
          .perform(
              post("/api/me/api-keys")
                  .header("Authorization", aliceToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"label\":\"my-key\"}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").isNumber())
          .andExpect(jsonPath("$.label").value("my-key"))
          .andExpect(jsonPath("$.prefix").value(org.hamcrest.Matchers.startsWith("bk_")))
          .andExpect(jsonPath("$.key").value(org.hamcrest.Matchers.startsWith("bk_")))
          .andExpect(jsonPath("$.key").value(org.hamcrest.Matchers.hasLength(35)))
          .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void label이_빈문자열이면_400() throws Exception {
      mockMvc
          .perform(
              post("/api/me/api-keys")
                  .header("Authorization", aliceToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"label\":\"\"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void label이_공백만이면_400() throws Exception {
      mockMvc
          .perform(
              post("/api/me/api-keys")
                  .header("Authorization", aliceToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"label\":\"   \"}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void label이_51자면_400() throws Exception {
      String label = "a".repeat(51);
      mockMvc
          .perform(
              post("/api/me/api-keys")
                  .header("Authorization", aliceToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"label\":\"" + label + "\"}"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  class 목록 {

    @Test
    void JWT_없으면_401() throws Exception {
      mockMvc.perform(get("/api/me/api-keys")).andExpect(status().isUnauthorized());
    }

    @Test
    void 본인_키만_생성순_역순으로_반환하고_평문_key는_미포함() throws Exception {
      apiKeyService.issue("alice", "first");
      Thread.sleep(20);
      apiKeyService.issue("alice", "second");
      apiKeyService.issue("bob", "bob-key");

      mockMvc
          .perform(get("/api/me/api-keys").header("Authorization", aliceToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].label").value("second"))
          .andExpect(jsonPath("$[1].label").value("first"))
          .andExpect(jsonPath("$[0].prefix").value(org.hamcrest.Matchers.startsWith("bk_")))
          .andExpect(jsonPath("$[0].key").doesNotExist())
          .andExpect(jsonPath("$[1].key").doesNotExist());
    }
  }

  @Nested
  class 폐기 {

    @Test
    void JWT_없으면_401() throws Exception {
      mockMvc.perform(delete("/api/me/api-keys/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void 본인_키면_204이고_이후_목록에서_revokedAt이_보인다() throws Exception {
      ApiKey issued = apiKeyService.issue("alice", "k").apiKey();

      mockMvc
          .perform(delete("/api/me/api-keys/" + issued.getId()).header("Authorization", aliceToken))
          .andExpect(status().isNoContent());

      ApiKey reloaded = apiKeyRepository.findById(issued.getId()).orElseThrow();
      assertThat(reloaded.getRevokedAt()).isNotNull();
    }

    @Test
    void 타인의_키_id로_폐기_시도하면_404() throws Exception {
      ApiKey issued = apiKeyService.issue("alice", "k").apiKey();

      mockMvc
          .perform(delete("/api/me/api-keys/" + issued.getId()).header("Authorization", bobToken))
          .andExpect(status().isNotFound());

      ApiKey reloaded = apiKeyRepository.findById(issued.getId()).orElseThrow();
      assertThat(reloaded.getRevokedAt()).isNull();
    }

    @Test
    void 존재하지_않는_id면_404() throws Exception {
      mockMvc
          .perform(delete("/api/me/api-keys/9999").header("Authorization", aliceToken))
          .andExpect(status().isNotFound());
    }

    @Test
    void 이미_폐기된_키를_재폐기해도_204이고_revokedAt은_변하지_않는다() throws Exception {
      ApiKey issued = apiKeyService.issue("alice", "k").apiKey();
      mockMvc
          .perform(delete("/api/me/api-keys/" + issued.getId()).header("Authorization", aliceToken))
          .andExpect(status().isNoContent());
      java.time.Instant first =
          apiKeyRepository.findById(issued.getId()).orElseThrow().getRevokedAt();

      mockMvc
          .perform(delete("/api/me/api-keys/" + issued.getId()).header("Authorization", aliceToken))
          .andExpect(status().isNoContent());

      java.time.Instant second =
          apiKeyRepository.findById(issued.getId()).orElseThrow().getRevokedAt();
      assertThat(second).isEqualTo(first);
    }
  }
}
