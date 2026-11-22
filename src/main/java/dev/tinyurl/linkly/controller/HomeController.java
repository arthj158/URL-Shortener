package dev.tinyurl.linkly.controller;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sends visitors at the service root to the API's interactive documentation. */
@RestController
public class HomeController {
  @GetMapping("/")
  public ResponseEntity<Void> home() {
    return ResponseEntity.status(302)
        .header(HttpHeaders.LOCATION, URI.create("/swagger-ui/index.html").toString())
        .build();
  }
}
