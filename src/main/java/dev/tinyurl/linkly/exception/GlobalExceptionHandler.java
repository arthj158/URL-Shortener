package dev.tinyurl.linkly.exception;

import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  public record ErrorResponse(
      Instant timestamp,
      int status,
      String error,
      String code,
      String message,
      Map<String, String> fields) {}

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ErrorResponse> api(ApiException e) {
    return response(e.status(), e.code(), e.getMessage(), Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(x -> fields.putIfAbsent(x.getField(), x.getDefaultMessage()));
    return response(
        HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid", fields);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
  ResponseEntity<ErrorResponse> badRequest(Exception e) {
    return response(
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request body or parameter is invalid", Map.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception e) {
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        Map.of());
  }

  private ResponseEntity<ErrorResponse> response(
      HttpStatus s, String code, String msg, Map<String, String> fields) {
    return ResponseEntity.status(s)
        .body(new ErrorResponse(Instant.now(), s.value(), s.getReasonPhrase(), code, msg, fields));
  }
}
