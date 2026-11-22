package dev.tinyurl.linkly.service;

import dev.tinyurl.linkly.dto.AuthDtos.*;
import dev.tinyurl.linkly.entity.AppUser;
import dev.tinyurl.linkly.exception.ApiException;
import dev.tinyurl.linkly.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  public Token register(Credentials c) {
    String email = c.email().toLowerCase().trim();
    if (users.existsByEmail(email))
      throw new ApiException(
          HttpStatus.CONFLICT, "EMAIL_EXISTS", "An account with this email already exists");
    users.save(new AppUser(email, encoder.encode(c.password())));
    return token(email);
  }

  public Token login(Credentials c) {
    String email = c.email().toLowerCase().trim();
    AppUser user =
        users
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS",
                        "Email or password is incorrect"));
    if (!encoder.matches(c.password(), user.getPasswordHash()))
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect");
    return token(email);
  }

  private Token token(String email) {
    return new Token(jwt.issue(email), "Bearer", jwt.expirationSeconds());
  }
}
