package dev.tinyurl.linkly.service;

import dev.tinyurl.linkly.entity.*;
import dev.tinyurl.linkly.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
  private final LinkRepository links;
  private final ClickEventRepository events;

  public AnalyticsService(LinkRepository links, ClickEventRepository events) {
    this.links = links;
    this.events = events;
  }

  @Async
  @Transactional
  public void record(long linkId, String ip, String referrer, String ua, String country) {
    ShortLink link = links.getReferenceById(linkId);
    String agent = ua == null ? "" : ua;
    events.save(
        new ClickEvent(
            link,
            hash(ip + "|" + agent),
            trim(referrer, 2000),
            trim(ua, 2000),
            browser(agent),
            device(agent),
            country));
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private String browser(String ua) {
    if (ua.contains("Edg/")) return "Edge";
    if (ua.contains("Firefox/")) return "Firefox";
    if (ua.contains("Chrome/")) return "Chrome";
    if (ua.contains("Safari/")) return "Safari";
    return ua.isBlank() ? "Unknown" : "Other";
  }

  private String device(String ua) {
    String s = ua.toLowerCase();
    if (s.contains("ipad") || s.contains("tablet")) return "Tablet";
    if (s.contains("mobile") || s.contains("iphone") || s.contains("android")) return "Mobile";
    return ua.isBlank() ? "Unknown" : "Desktop";
  }

  private String trim(String s, int max) {
    return s == null ? null : s.substring(0, Math.min(max, s.length()));
  }
}
