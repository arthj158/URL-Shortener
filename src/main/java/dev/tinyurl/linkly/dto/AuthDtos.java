package dev.tinyurl.linkly.dto;

import jakarta.validation.constraints.*;

public final class AuthDtos {
  private AuthDtos() {}

  public record Credentials(
      @Email @NotBlank @Size(max = 254) String email,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record Token(String token, String tokenType, long expiresInSeconds) {}
}
