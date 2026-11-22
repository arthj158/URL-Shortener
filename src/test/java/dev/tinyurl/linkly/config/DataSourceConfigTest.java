package dev.tinyurl.linkly.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

class DataSourceConfigTest {
  @Test
  void convertsRenderPostgresUrlIntoJdbcConfiguration() {
    DataSourceProperties properties = new DataSourceProperties();
    properties.setUrl(
        "postgresql://render_user:p%40ss+word@db.example:5433/linkly?sslmode=require");

    var dataSource = new DataSourceConfig().dataSource(properties);

    assertEquals(
        "jdbc:postgresql://db.example:5433/linkly?sslmode=require", dataSource.getJdbcUrl());
    assertEquals("render_user", dataSource.getUsername());
    assertEquals("p@ss+word", dataSource.getPassword());
    dataSource.close();
  }
}
