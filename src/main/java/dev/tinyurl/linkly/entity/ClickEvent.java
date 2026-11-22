package dev.tinyurl.linkly.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "click_events")
public class ClickEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "link_id", nullable = false)
  private ShortLink link;

  @Column(name = "clicked_at", nullable = false)
  private Instant clickedAt = Instant.now();

  @Column(name = "visitor_hash", nullable = false, length = 64)
  private String visitorHash;

  @Column(columnDefinition = "text")
  private String referrer;

  @Column(name = "user_agent", columnDefinition = "text")
  private String userAgent;

  @Column(length = 80)
  private String browser;

  @Column(name = "device_type", length = 40)
  private String deviceType;

  @Column(length = 2)
  private String country;

  protected ClickEvent() {}

  public ClickEvent(
      ShortLink link,
      String visitorHash,
      String referrer,
      String userAgent,
      String browser,
      String deviceType,
      String country) {
    this.link = link;
    this.visitorHash = visitorHash;
    this.referrer = referrer;
    this.userAgent = userAgent;
    this.browser = browser;
    this.deviceType = deviceType;
    this.country = country;
  }
}
