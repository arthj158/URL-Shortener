package dev.tinyurl.linkly.controller;

import dev.tinyurl.linkly.dto.LinkDtos.*;
import dev.tinyurl.linkly.service.LinkService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/links")
public class LinkController {
  private final LinkService service;

  public LinkController(LinkService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public LinkResponse create(
      @AuthenticationPrincipal UserDetails user, @Valid @RequestBody CreateRequest req) {
    return service.create(user.getUsername(), req);
  }

  @GetMapping
  public Page<LinkResponse> list(
      @AuthenticationPrincipal UserDetails user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    String[] p = sort.split(",");
    if (p.length > 2 || !List.of("createdAt", "expiresAt", "code").contains(p[0]))
      throw new dev.tinyurl.linkly.exception.ApiException(
          HttpStatus.BAD_REQUEST, "INVALID_SORT", "Sort by createdAt, expiresAt, or code");
    Sort.Direction direction =
        p.length > 1 && p[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    if (p.length > 1 && !List.of("asc", "desc").contains(p[1].toLowerCase()))
      throw new dev.tinyurl.linkly.exception.ApiException(
          HttpStatus.BAD_REQUEST, "INVALID_SORT", "Sort direction must be asc or desc");
    return service.list(
        user.getUsername(),
        PageRequest.of(
            Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(direction, p[0])));
  }

  @GetMapping("/{code}/stats")
  public Stats stats(@AuthenticationPrincipal UserDetails user, @PathVariable String code) {
    return service.stats(service.owned(user.getUsername(), code));
  }

  @GetMapping("/{code}/browsers")
  public List<Breakdown> browsers(
      @AuthenticationPrincipal UserDetails user, @PathVariable String code) {
    return service.breakdown(service.owned(user.getUsername(), code), "browsers");
  }

  @GetMapping("/{code}/devices")
  public List<Breakdown> devices(
      @AuthenticationPrincipal UserDetails user, @PathVariable String code) {
    return service.breakdown(service.owned(user.getUsername(), code), "devices");
  }

  @GetMapping("/{code}/referrers")
  public List<Breakdown> referrers(
      @AuthenticationPrincipal UserDetails user, @PathVariable String code) {
    return service.breakdown(service.owned(user.getUsername(), code), "referrers");
  }
}
