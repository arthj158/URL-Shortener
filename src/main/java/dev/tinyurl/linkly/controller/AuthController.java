package dev.tinyurl.linkly.controller;

import dev.tinyurl.linkly.dto.AuthDtos.*;
import dev.tinyurl.linkly.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Token register(@Valid @RequestBody Credentials body) {
    return auth.register(body);
  }

  @PostMapping("/login")
  public Token login(@Valid @RequestBody Credentials body) {
    return auth.login(body);
  }
}
