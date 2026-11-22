package dev.tinyurl.linkly.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "links")
public class ShortLink {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Column(name = "original_url", nullable = false, columnDefinition = "text")
  private String originalUrl;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  protected ShortLink() {}

  public ShortLink(String code, String originalUrl, Instant expiresAt, AppUser user) {
    this.code = code;
    this.originalUrl = originalUrl;
    this.expiresAt = expiresAt;
    this.user = user;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public AppUser getUser() {
    return user;
  }
}
