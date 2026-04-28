package com.example.board.user;

import java.time.Instant;

public final class ApiKeyResponses {

  private ApiKeyResponses() {}

  public record IssuedApiKeyResponse(
      Long id, String label, String prefix, String key, Instant createdAt) {

    public static IssuedApiKeyResponse from(ApiKeyService.IssueResult result) {
      ApiKey apiKey = result.apiKey();
      return new IssuedApiKeyResponse(
          apiKey.getId(),
          apiKey.getLabel(),
          apiKey.getPrefix(),
          result.plainKey(),
          apiKey.getCreatedAt());
    }
  }

  public record ApiKeySummaryResponse(
      Long id,
      String label,
      String prefix,
      Instant lastUsedAt,
      Instant createdAt,
      Instant revokedAt) {

    public static ApiKeySummaryResponse from(ApiKey apiKey) {
      return new ApiKeySummaryResponse(
          apiKey.getId(),
          apiKey.getLabel(),
          apiKey.getPrefix(),
          apiKey.getLastUsedAt(),
          apiKey.getCreatedAt(),
          apiKey.getRevokedAt());
    }
  }
}
