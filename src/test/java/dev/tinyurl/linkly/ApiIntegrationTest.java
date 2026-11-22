package dev.tinyurl.linkly;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:linkly;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.cache.type=simple",
      "app.jwt.secret=01234567890123456789012345678901"
    })
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void healthEndpointIsPublicForRenderChecks() throws Exception {
    mvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void protectedApiUsesConsistentUnauthorizedFormat() throws Exception {
    mvc.perform(get("/api/links"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void registerCreateRedirectAndReadStats() throws Exception {
    String token =
        json.readTree(
                mvc.perform(
                        post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                "{\"email\":\"person@example.com\",\"password\":\"good-password-123\"}"))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("token")
            .asText();
    String created =
        mvc.perform(
                post("/api/links")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"url\":\"https://example.org/article\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String code = json.readTree(created).get("code").asText();
    mvc.perform(
            get("/" + code)
                .header("User-Agent", "Mozilla/5.0 Chrome/120")
                .header("Referer", "https://search.example/"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "https://example.org/article"));
    String login =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"person@example.com\",\"password\":\"good-password-123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.junit.jupiter.api.Assertions.assertNotNull(json.readTree(login).get("token"));
    String stats = "";
    for (int i = 0; i < 20; i++) {
      stats =
          mvc.perform(
                  get("/api/links/" + code + "/stats").header("Authorization", "Bearer " + token))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      if (json.readTree(stats).get("totalClicks").asLong() > 0) break;
      Thread.sleep(50);
    }
    org.junit.jupiter.api.Assertions.assertEquals(
        1, json.readTree(stats).get("totalClicks").asLong());
  }
}
