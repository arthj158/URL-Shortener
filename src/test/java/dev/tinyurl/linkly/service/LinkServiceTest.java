package dev.tinyurl.linkly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.tinyurl.linkly.dto.LinkDtos.CreateRequest;
import dev.tinyurl.linkly.entity.*;
import dev.tinyurl.linkly.repository.*;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LinkServiceTest {
  @Test
  void generatedCodesEncodeAllocatedId() {
    var links = mock(LinkRepository.class);
    var users = mock(UserRepository.class);
    var clicks = mock(ClickEventRepository.class);
    var user = new AppUser("a@b.com", "hash");
    ReflectionTestUtils.setField(user, "id", 1L);
    when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(links.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              ShortLink link = inv.getArgument(0);
              ReflectionTestUtils.setField(link, "id", 62L);
              return link;
            });
    var service = new LinkService(links, users, clicks, "http://localhost:8080", 20, 60);
    var out = service.create("a@b.com", new CreateRequest("https://example.org", null, null));
    assertEquals("10", out.code());
    verify(links).saveAndFlush(any());
  }

  @Test
  void rejectsUnsafeSchemes() {
    var links = mock(LinkRepository.class);
    var users = mock(UserRepository.class);
    var service =
        new LinkService(
            links, users, mock(ClickEventRepository.class), "http://localhost:8080", 20, 60);
    when(users.findByEmail("a@b.com")).thenReturn(Optional.of(new AppUser("a@b.com", "hash")));
    assertThrows(
        RuntimeException.class,
        () -> service.create("a@b.com", new CreateRequest("javascript:alert(1)", null, null)));
    verify(links, never()).saveAndFlush(any());
  }

  @Test
  void expiryBoundaryIsGone() {
    var service =
        new LinkService(
            mock(LinkRepository.class),
            mock(UserRepository.class),
            mock(ClickEventRepository.class),
            "http://localhost:8080",
            20,
            60);
    assertThrows(
        RuntimeException.class,
        () ->
            service.ensureActive(
                new LinkService.RedirectTarget(
                    1, "https://x.test", Instant.now().minusSeconds(1))));
    assertDoesNotThrow(
        () ->
            service.ensureActive(
                new LinkService.RedirectTarget(1, "https://x.test", Instant.now().plusSeconds(1))));
  }
}
