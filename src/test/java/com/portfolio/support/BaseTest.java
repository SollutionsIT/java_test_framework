package com.portfolio.support;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import com.portfolio.api.TasksClient;
import com.portfolio.config.TestConfig;
import com.portfolio.demo.DemoServer;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {
  protected TestConfig config;
  protected Playwright playwright;
  protected APIRequestContext request;
  protected TasksClient tasks;
  private DemoServer demo;

  @BeforeAll
  void startEnvironment() throws IOException {
    config = TestConfig.load();
    if (config.demoMode()) {
      demo = new DemoServer(config.baseUrl());
      demo.start();
    }
    playwright = Playwright.create();
  }

  @BeforeEach
  void createApiContext() {
    request =
        playwright
            .request()
            .newContext(
                new APIRequest.NewContextOptions()
                    .setBaseURL(config.baseUrl())
                    .setTimeout(config.timeoutMs())
                    .setExtraHTTPHeaders(Map.of("Accept", "application/json")));
    tasks = new TasksClient(request);
  }

  @AfterEach
  void disposeApiContext() {
    if (request != null) {
      request.dispose();
    }
  }

  @AfterAll
  void stopEnvironment() {
    try {
      if (playwright != null) {
        playwright.close();
      }
    } finally {
      if (demo != null) {
        demo.close();
      }
    }
  }
}
