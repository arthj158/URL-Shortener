package dev.tinyurl.linkly.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tinyurl.linkly.exception.GlobalExceptionHandler;
import dev.tinyurl.linkly.repository.UserRepository;
import dev.tinyurl.linkly.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return email ->
        users
            .findByEmail(email)
            .map(
                u ->
                    User.withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter, ObjectMapper mapper)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/health",
                        "/api/auth/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, error) ->
                            writeError(
                                mapper, res, 401, "UNAUTHORIZED", "Authentication is required"))
                    .accessDeniedHandler(
                        (req, res, error) ->
                            writeError(
                                mapper,
                                res,
                                403,
                                "FORBIDDEN",
                                "You do not have access to this resource")))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private void writeError(
      ObjectMapper mapper, HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    mapper.writeValue(
        response.getOutputStream(),
        new GlobalExceptionHandler.ErrorResponse(
            Instant.now(),
            status,
            status == 401 ? "Unauthorized" : "Forbidden",
            code,
            message,
            Map.of()));
  }

  @Bean
  JwtFilter jwtFilter(JwtService jwt, UserDetailsService users) {
    return new JwtFilter(jwt, users);
  }

  static class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserDetailsService users;

    JwtFilter(JwtService jwt, UserDetailsService users) {
      this.jwt = jwt;
      this.users = users;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
      String h = req.getHeader("Authorization");
      if (h != null && h.startsWith("Bearer "))
        try {
          String subject = jwt.subject(h.substring(7));
          if (subject != null
              && org.springframework.security.core.context.SecurityContextHolder.getContext()
                      .getAuthentication()
                  == null) {
            var principal = users.loadUserByUsername(subject);
            var auth =
                new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(auth);
          }
        } catch (RuntimeException ignored) {
        }
      chain.doFilter(req, res);
    }
  }
}
