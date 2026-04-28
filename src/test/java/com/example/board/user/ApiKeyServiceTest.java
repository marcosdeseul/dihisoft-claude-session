package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.board.user.ApiKeyService.IssueResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class ApiKeyServiceTest {

  @Autowired ApiKeyService apiKeyService;
  @Autowired ApiKeyRepository apiKeyRepository;
  @Autowired UserRepository userRepository;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    apiKeyRepository.deleteAll();
    userRepository.deleteAll();
    userRepository.save(new User("alice", passwordEncoder.encode("pw12345")));
    userRepository.save(new User("bob", passwordEncoder.encode("pw12345")));
  }

  @AfterEach
  void tearDown() {
    apiKeyRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  class 발급 {

    @Test
    void 정상_label이면_bk_접두와_총_35자_평문_key를_반환한다() {
      IssueResult result = apiKeyService.issue("alice", "for tests");
      assertThat(result.plainKey()).startsWith("bk_").hasSize(35);
    }

    @Test
    void 발급된_엔티티의_prefix는_평문_앞_11자와_일치한다() {
      IssueResult result = apiKeyService.issue("alice", "for tests");
      assertThat(result.apiKey().getPrefix()).isEqualTo(result.plainKey().substring(0, 11));
    }

    @Test
    void 저장된_hashedKey는_평문과_다르고_64자_hex이다() {
      IssueResult result = apiKeyService.issue("alice", "for tests");
      ApiKey saved = apiKeyRepository.findById(result.apiKey().getId()).orElseThrow();
      assertThat(saved.getHashedKey()).isNotEqualTo(result.plainKey());
      assertThat(saved.getHashedKey()).matches("[0-9a-f]{64}");
    }

    @Test
    void label이_null이면_IllegalArgumentException() {
      assertThatThrownBy(() -> apiKeyService.issue("alice", null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void label이_빈문자열이면_IllegalArgumentException() {
      assertThatThrownBy(() -> apiKeyService.issue("alice", ""))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void label이_공백만이면_IllegalArgumentException() {
      assertThatThrownBy(() -> apiKeyService.issue("alice", "   "))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void label이_51자면_IllegalArgumentException() {
      String label = "a".repeat(51);
      assertThatThrownBy(() -> apiKeyService.issue("alice", label))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 없는_username이면_IllegalArgumentException() {
      assertThatThrownBy(() -> apiKeyService.issue("nope", "label"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class 인증 {

    @Test
    void 정상_key이면_소유자_username을_반환한다() {
      IssueResult issued = apiKeyService.issue("alice", "k");
      Optional<String> resolved = apiKeyService.authenticate(issued.plainKey());
      assertThat(resolved).contains("alice");
    }

    @Test
    void 정상_key이면_lastUsedAt이_갱신된다() {
      IssueResult issued = apiKeyService.issue("alice", "k");
      Instant before = Instant.now();
      apiKeyService.authenticate(issued.plainKey());
      ApiKey reloaded = apiKeyRepository.findById(issued.apiKey().getId()).orElseThrow();
      assertThat(reloaded.getLastUsedAt()).isNotNull();
      assertThat(reloaded.getLastUsedAt()).isAfterOrEqualTo(before.minusSeconds(1));
    }

    @Test
    void null이면_empty() {
      assertThat(apiKeyService.authenticate(null)).isEmpty();
    }

    @Test
    void 빈_문자열이면_empty() {
      assertThat(apiKeyService.authenticate("")).isEmpty();
    }

    @Test
    void bk_접두가_없으면_empty() {
      assertThat(apiKeyService.authenticate("xx_" + "a".repeat(32))).isEmpty();
    }

    @Test
    void 길이가_35자가_아니면_empty() {
      assertThat(apiKeyService.authenticate("bk_short")).isEmpty();
    }

    @Test
    void prefix는_맞지만_hash가_다르면_empty() {
      IssueResult issued = apiKeyService.issue("alice", "k");
      String tampered = issued.plainKey().substring(0, 11) + "z".repeat(24);
      assertThat(apiKeyService.authenticate(tampered)).isEmpty();
    }

    @Test
    void revoked된_key면_empty() {
      IssueResult issued = apiKeyService.issue("alice", "k");
      apiKeyService.revoke("alice", issued.apiKey().getId());
      assertThat(apiKeyService.authenticate(issued.plainKey())).isEmpty();
    }
  }

  @Nested
  class 폐기 {

    @Test
    void 본인_키이면_revokedAt이_세팅된다() {
      IssueResult issued = apiKeyService.issue("alice", "k");
      apiKeyService.revoke("alice", issued.apiKey().getId());
      ApiKey reloaded = apiKeyRepository.findById(issued.apiKey().getId()).orElseThrow();
      assertThat(reloaded.getRevokedAt()).isNotNull();
    }

    @Test
    void 이미_폐기된_키를_재폐기해도_예외없이_revokedAt이_변하지_않는다() {
      IssueResult issued = apiKeyService.issue("alice", "k");
      apiKeyService.revoke("alice", issued.apiKey().getId());
      Instant first =
          apiKeyRepository.findById(issued.apiKey().getId()).orElseThrow().getRevokedAt();

      apiKeyService.revoke("alice", issued.apiKey().getId());

      Instant second =
          apiKeyRepository.findById(issued.apiKey().getId()).orElseThrow().getRevokedAt();
      assertThat(second).isEqualTo(first);
    }

    @Test
    void 타인의_키_id로_폐기_시도하면_404() {
      IssueResult issued = apiKeyService.issue("alice", "k");
      assertThatThrownBy(() -> apiKeyService.revoke("bob", issued.apiKey().getId()))
          .isInstanceOf(ResponseStatusException.class)
          .satisfies(
              e ->
                  assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void 존재하지_않는_id로_폐기_시도하면_404() {
      assertThatThrownBy(() -> apiKeyService.revoke("alice", 9999L))
          .isInstanceOf(ResponseStatusException.class)
          .satisfies(
              e ->
                  assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }
  }

  @Nested
  class 목록 {

    @Test
    void 본인_키만_생성순_역순으로_반환한다() throws Exception {
      ApiKey a1 = apiKeyService.issue("alice", "first").apiKey();
      Thread.sleep(20);
      ApiKey a2 = apiKeyService.issue("alice", "second").apiKey();
      apiKeyService.issue("bob", "bob-key");

      List<ApiKey> keys = apiKeyService.listFor("alice");

      assertThat(keys).extracting(ApiKey::getId).containsExactly(a2.getId(), a1.getId());
    }

    @Test
    void 발급한_키가_없으면_빈_리스트() {
      assertThat(apiKeyService.listFor("alice")).isEmpty();
    }
  }
}
