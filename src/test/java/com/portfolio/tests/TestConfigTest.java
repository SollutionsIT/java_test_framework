package com.portfolio.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.portfolio.config.TestConfig;
import java.net.URI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
class TestConfigTest {
  @Test
  void shouldPreserveBasePathForRelativeEndpoints() {
    TestConfig config = new TestConfig("https://example.test/qa", false, true, 1000);
    assertEquals(
        "https://example.test/qa/api/tasks",
        URI.create(config.baseUrl()).resolve("api/tasks").toString());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "localhost:8080",
        "ftp://example.test",
        "https://example.test/?q=1",
        "https://user:pass@example.test",
        "https://example.test/#test"
      })
  void shouldRejectInvalidBaseUrl(String value) {
    assertThrows(IllegalArgumentException.class, () -> new TestConfig(value, false, true, 1000));
  }

  @Test
  void shouldRejectNonPositiveTimeout() {
    assertThrows(
        IllegalArgumentException.class, () -> new TestConfig("http://localhost", false, true, 0));
  }
}
