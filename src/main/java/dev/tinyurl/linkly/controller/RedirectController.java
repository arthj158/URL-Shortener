package dev.tinyurl.linkly.controller;

import dev.tinyurl.linkly.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class RedirectController {
  private final LinkService links;
  private final AnalyticsService analytics;

  public RedirectController(LinkService links, AnalyticsService analytics) {
    this.links = links;
    this.analytics = analytics;
  }

  @GetMapping("/{code:[A-Za-z0-9_-]{1,32}}")
  public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
    var target = links.lookup(code);
    links.ensureActive(target);
    analytics.record(
        target.id(),
        request.getRemoteAddr(),
        request.getHeader("Referer"),
        request.getHeader("User-Agent"),
        request.getHeader("CF-IPCountry"));
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(java.net.URI.create(target.url()))
        .cacheControl(CacheControl.noStore())
        .build();
  }
}
