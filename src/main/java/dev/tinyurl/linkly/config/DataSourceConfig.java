package dev.tinyurl.linkly.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {
  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  HikariDataSource dataSource(DataSourceProperties properties) {
    String url = properties.getUrl();
    if (url != null && (url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
      URI uri = URI.create(url);
      String path = uri.getRawPath();
      if (uri.getHost() == null || path == null || path.length() < 2) {
        throw new IllegalStateException(
            "DATABASE_URL must include a PostgreSQL host and database name");
      }

      int port = uri.getPort() == -1 ? 5432 : uri.getPort();
      String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + path;
      if (uri.getRawQuery() != null) {
        jdbcUrl += "?" + uri.getRawQuery();
      }
      properties.setUrl(jdbcUrl);

      String userInfo = uri.getRawUserInfo();
      if (userInfo != null) {
        int separator = userInfo.indexOf(':');
        if (separator >= 0) {
          properties.setUsername(decode(userInfo.substring(0, separator)));
          properties.setPassword(decode(userInfo.substring(separator + 1)));
        } else {
          properties.setUsername(decode(userInfo));
        }
      }
    }

    return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  private String decode(String value) {
    // URLDecoder treats literal '+' as a space; preserve it unless it was percent-encoded.
    return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
  }
}
