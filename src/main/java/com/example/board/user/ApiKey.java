package com.example.board.user;

import com.example.board.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "api_keys",
    indexes = {@Index(name = "idx_api_keys_prefix", columnList = "prefix")})
public class ApiKey extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 50)
  private String label;

  @Column(name = "hashed_key", nullable = false, unique = true, length = 64)
  private String hashedKey;

  @Column(nullable = false, length = 11)
  private String prefix;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  protected ApiKey() {}

  private ApiKey(User user, String label, String prefix, String hashedKey) {
    this.user = user;
    this.label = label;
    this.prefix = prefix;
    this.hashedKey = hashedKey;
  }

  public static ApiKey create(User user, String label, String prefix, String hashedKey) {
    return new ApiKey(user, label, prefix, hashedKey);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public void markUsed(Instant when) {
    this.lastUsedAt = when;
  }

  public void revoke(Instant when) {
    if (revokedAt == null) {
      this.revokedAt = when;
    }
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getLabel() {
    return label;
  }

  public String getHashedKey() {
    return hashedKey;
  }

  public String getPrefix() {
    return prefix;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }
}
