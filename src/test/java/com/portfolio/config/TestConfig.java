package com.portfolio.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

public record TestConfig(String baseUrl, boolean demoMode, boolean headless, int timeoutMs) {
  public TestConfig {
    URI uri = URI.create(baseUrl);
    if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null) {
      throw new IllegalArgumentException(
          "BASE_URL must be an absolute HTTP(S) URL without credentials/query/fragment");
    }
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("TIMEOUT_MS must be positive");
    }
    baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
  }

  public static TestConfig load() {
    Properties defaults = new Properties();
    try (var input = TestConfig.class.getResourceAsStream("/test.properties")) {
      if (input == null) {
        throw new IllegalStateException("Missing test.properties");
      }
      defaults.load(input);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read test.properties", e);
    }
    Dotenv env = Dotenv.configure().ignoreIfMissing().load();
    return new TestConfig(
        value("BASE_URL", env, defaults),
        bool(value("DEMO_MODE", env, defaults)),
        bool(value("HEADLESS", env, defaults)),
        Integer.parseInt(value("TIMEOUT_MS", env, defaults)));
  }

  private static String value(String key, Dotenv env, Properties defaults) {
    return System.getProperty(key, env.get(key, defaults.getProperty(key)));
  }

  private static boolean bool(String value) {
    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
      throw new IllegalArgumentException("Expected true or false, got: " + value);
    }
    return Boolean.parseBoolean(value);
  }
}
