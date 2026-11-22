package dev.tinyurl.linkly.service;

import dev.tinyurl.linkly.dto.LinkDtos.*;
import dev.tinyurl.linkly.entity.*;
import dev.tinyurl.linkly.exception.ApiException;
import dev.tinyurl.linkly.repository.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {
  private final LinkRepository links;
  private final UserRepository users;
  private final ClickEventRepository clicks;
  private final String baseUrl;
  private final int maxRequests;
  private final long windowMillis;
  private final Map<Long, Window> limits = new ConcurrentHashMap<>();

  private record Window(long start, int count) {}

  public LinkService(
      LinkRepository links,
      UserRepository users,
      ClickEventRepository clicks,
      @Value("${app.public-base-url}") String baseUrl,
      @Value("${app.rate-limit.max-requests}") int maxRequests,
      @Value("${app.rate-limit.window-seconds}") long windowSeconds) {
    this.links = links;
    this.users = users;
    this.clicks = clicks;
    this.baseUrl = baseUrl.replaceAll("/$", "");
    this.maxRequests = maxRequests;
    this.windowMillis = windowSeconds * 1000;
  }

  @Transactional
  public LinkResponse create(String email, CreateRequest req) {
    AppUser user = user(email);
    validateUrl(req.url());
    String code = req.alias();
    if (code != null && links.existsByCode(code))
      throw new ApiException(HttpStatus.CONFLICT, "ALIAS_TAKEN", "That alias is already in use");
    checkLimit(user.getId());
    // A temporary UUID occupies the unique column while the DB assigns its primary key. We then
    // encode that key; the primary-key uniqueness guarantees generated codes are unique.
    ShortLink link =
        links.saveAndFlush(
            new ShortLink(
                UUID.randomUUID().toString().replace("-", ""),
                req.url().trim(),
                req.expiresAt(),
                user));
    if (code == null) {
      code = Base62.encode(link.getId());
      link.setCode(code);
    }
    return response(link, 0);
  }

  private void checkLimit(long id) {
    long now = System.currentTimeMillis();
    synchronized (limits) {
      Window w = limits.get(id);
      if (w == null || now - w.start() >= windowMillis) {
        limits.put(id, new Window(now, 1));
        return;
      }
      if (w.count() >= maxRequests)
        throw new ApiException(
            HttpStatus.TOO_MANY_REQUESTS,
            "RATE_LIMITED",
            "Link creation limit reached; try again shortly");
      limits.put(id, new Window(w.start(), w.count() + 1));
    }
  }

  private void validateUrl(String value) {
    try {
      URI uri = URI.create(value.trim());
      String scheme = uri.getScheme();
      if (!uri.isAbsolute()
          || uri.getHost() == null
          || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          || uri.getUserInfo() != null) throw new IllegalArgumentException();
    } catch (RuntimeException e) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "INVALID_URL", "URL must be a valid HTTP or HTTPS address");
    }
  }

  private AppUser user(String email) {
    return users
        .findByEmail(email)
        .orElseThrow(
            () -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Account not found"));
  }

  @Transactional(readOnly = true)
  public Page<LinkResponse> list(String email, Pageable pageable) {
    AppUser u = user(email);
    return links
        .findOwnedWithClicks(u.getId(), pageable)
        .map(row -> response((ShortLink) row[0], ((Number) row[1]).longValue()));
  }

  @Transactional(readOnly = true)
  public ShortLink owned(String email, String code) {
    AppUser u = user(email);
    ShortLink l =
        links
            .findByCode(code)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "LINK_NOT_FOUND", "Link not found"));
    if (!l.getUser().getId().equals(u.getId()))
      throw new ApiException(HttpStatus.NOT_FOUND, "LINK_NOT_FOUND", "Link not found");
    return l;
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "redirects", key = "#code")
  public RedirectTarget lookup(String code) {
    ShortLink l =
        links
            .findByCode(code)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "LINK_NOT_FOUND", "Link not found"));
    return new RedirectTarget(l.getId(), l.getOriginalUrl(), l.getExpiresAt());
  }

  public void ensureActive(RedirectTarget target) {
    if (target.expiresAt() != null && !target.expiresAt().isAfter(Instant.now()))
      throw new ApiException(HttpStatus.GONE, "LINK_EXPIRED", "This link has expired");
  }

  @Transactional(readOnly = true)
  public Stats stats(ShortLink l) {
    return new Stats(
        clicks.total(l.getId()),
        clicks.uniqueVisitors(l.getId()),
        clicks
            .clicksPerDay(l.getId(), Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS))
            .stream()
            .map(r -> new DayClicks(r[0].toString(), ((Number) r[1]).longValue()))
            .toList());
  }

  @Transactional(readOnly = true)
  public List<Breakdown> breakdown(ShortLink l, String kind) {
    List<Object[]> rows =
        switch (kind) {
          case "browsers" -> clicks.browsers(l.getId());
          case "devices" -> clicks.devices(l.getId());
          default -> clicks.referrers(l.getId());
        };
    return rows.stream()
        .map(r -> new Breakdown((String) r[0], ((Number) r[1]).longValue()))
        .toList();
  }

  private LinkResponse response(ShortLink l, long n) {
    return new LinkResponse(
        l.getCode(),
        baseUrl + "/" + l.getCode(),
        l.getOriginalUrl(),
        l.getCreatedAt(),
        l.getExpiresAt(),
        n);
  }

  public record RedirectTarget(long id, String url, Instant expiresAt)
      implements java.io.Serializable {}
}
