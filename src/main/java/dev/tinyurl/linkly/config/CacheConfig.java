package dev.tinyurl.linkly.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class CacheConfig {
  @Bean
  @ConditionalOnProperty(name = "app.cache.type", havingValue = "redis")
  CacheManager redisCacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory)
        .cacheDefaults(
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(60)))
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "app.cache.type", havingValue = "caffeine", matchIfMissing = true)
  CacheManager caffeineCacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager("redirects");
    manager.setCaffeine(
        Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(60)).maximumSize(100_000));
    return manager;
  }
}
