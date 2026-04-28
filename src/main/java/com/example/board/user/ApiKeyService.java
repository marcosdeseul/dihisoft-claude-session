package com.example.board.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ApiKeyService {

  static final String KEY_PREFIX = "bk_";
  static final int KEY_BODY_LENGTH = 32;
  static final int KEY_TOTAL_LENGTH = KEY_PREFIX.length() + KEY_BODY_LENGTH;
  static final int STORED_PREFIX_LENGTH = KEY_PREFIX.length() + 8;
  private static final int LABEL_MAX_LENGTH = 50;
  private static final int RANDOM_BYTES = 24;
  private static final int MAX_PREFIX_RETRIES = 5;
  private static final String NOT_FOUND_MESSAGE = "API key를 찾을 수 없습니다";

  private final ApiKeyRepository apiKeyRepository;
  private final UserRepository userRepository;
  private final SecureRandom random = new SecureRandom();

  public ApiKeyService(ApiKeyRepository apiKeyRepository, UserRepository userRepository) {
    this.apiKeyRepository = apiKeyRepository;
    this.userRepository = userRepository;
  }

  public IssueResult issue(String username, String label) {
    String normalized = normalizeLabel(label);
    User owner = requireUser(username);

    for (int attempt = 0; attempt < MAX_PREFIX_RETRIES; attempt++) {
      String plain = generatePlainKey();
      String prefix = plain.substring(0, STORED_PREFIX_LENGTH);
      if (apiKeyRepository.existsByPrefix(prefix)) {
        continue;
      }
      String hashed = sha256Hex(plain);
      ApiKey saved = apiKeyRepository.save(ApiKey.create(owner, normalized, prefix, hashed));
      return new IssueResult(saved, plain);
    }
    throw new IllegalStateException("API key prefix 충돌이 반복되었습니다");
  }

  @Transactional(readOnly = true)
  public List<ApiKey> listFor(String username) {
    User owner = requireUser(username);
    return apiKeyRepository.findByUserOrderByCreatedAtDesc(owner);
  }

  public void revoke(String username, Long apiKeyId) {
    User owner =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));
    ApiKey key =
        apiKeyRepository
            .findByIdAndUser(apiKeyId, owner)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));
    key.revoke(Instant.now());
  }

  public Optional<String> authenticate(String rawKey) {
    if (rawKey == null || rawKey.length() != KEY_TOTAL_LENGTH || !rawKey.startsWith(KEY_PREFIX)) {
      return Optional.empty();
    }
    String prefix = rawKey.substring(0, STORED_PREFIX_LENGTH);
    Optional<ApiKey> found = apiKeyRepository.findByPrefix(prefix);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    ApiKey key = found.get();
    if (key.isRevoked()) {
      return Optional.empty();
    }
    String hashed = sha256Hex(rawKey);
    if (!MessageDigest.isEqual(
        hashed.getBytes(StandardCharsets.UTF_8),
        key.getHashedKey().getBytes(StandardCharsets.UTF_8))) {
      return Optional.empty();
    }
    key.markUsed(Instant.now());
    return Optional.of(key.getUser().getUsername());
  }

  private String normalizeLabel(String label) {
    if (label == null) {
      throw new IllegalArgumentException("label은 필수입니다");
    }
    String trimmed = label.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("label은 필수입니다");
    }
    if (trimmed.length() > LABEL_MAX_LENGTH) {
      throw new IllegalArgumentException("label은 " + LABEL_MAX_LENGTH + "자 이하여야 합니다");
    }
    return trimmed;
  }

  private User requireUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
  }

  private String generatePlainKey() {
    byte[] bytes = new byte[RANDOM_BYTES];
    random.nextBytes(bytes);
    String body = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return KEY_PREFIX + body;
  }

  private String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다", ex);
    }
  }

  public record IssueResult(ApiKey apiKey, String plainKey) {}
}
