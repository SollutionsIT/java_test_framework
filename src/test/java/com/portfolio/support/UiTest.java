package com.portfolio.support;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public abstract class UiTest extends BaseTest {
  protected Page page;
  private Browser browser;
  private BrowserContext context;

  @BeforeEach
  void openBrowser() {
    browser =
        playwright
            .chromium()
            .launch(new BrowserType.LaunchOptions().setHeadless(config.headless()));
    context =
        browser.newContext(
            new Browser.NewContextOptions()
                .setBaseURL(config.baseUrl())
                .setViewportSize(1280, 800));
    context.setDefaultTimeout(config.timeoutMs());
    context.setDefaultNavigationTimeout(config.timeoutMs());
    context
        .tracing()
        .start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
    page = context.newPage();
  }

  @AfterEach
  void saveArtifactsAndClose(TestInfo info) throws IOException {
    try {
      if (context != null) {
        String name = info.getTestMethod().orElseThrow().getName() + "-" + UUID.randomUUID();
        Path folder = Path.of("target", "ui-artifacts", name);
        Files.createDirectories(folder);
        try {
          if (page != null && !page.isClosed()) {
            page.screenshot(
                new Page.ScreenshotOptions()
                    .setPath(folder.resolve("screenshot.png"))
                    .setFullPage(true));
          }
        } finally {
          context.tracing().stop(new Tracing.StopOptions().setPath(folder.resolve("trace.zip")));
        }
      }
    } finally {
      if (browser != null) {
        browser.close();
      }
    }
  }
}
