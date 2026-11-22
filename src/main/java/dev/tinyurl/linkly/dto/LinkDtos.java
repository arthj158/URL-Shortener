package dev.tinyurl.linkly.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class LinkDtos {
  private LinkDtos() {}

  public record CreateRequest(
      @NotBlank @Size(max = 4096) String url,
      @Pattern(regexp = "^[A-Za-z0-9_-]{3,32}$", message = "must be 3-32 URL-safe characters")
          String alias,
      @Future Instant expiresAt) {}

  public record LinkResponse(
      String code,
      String shortUrl,
      String originalUrl,
      Instant createdAt,
      Instant expiresAt,
      long clicks) {}

  public record DayClicks(String date, long clicks) {}

  public record Stats(
      long totalClicks, long uniqueVisitors, java.util.List<DayClicks> clicksPerDay) {}

  public record Breakdown(String value, long count) {}
}
