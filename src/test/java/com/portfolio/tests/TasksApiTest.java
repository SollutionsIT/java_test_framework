package com.portfolio.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.portfolio.api.Endpoints;
import com.portfolio.model.Task;
import com.portfolio.support.BaseTest;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("api")
class TasksApiTest extends BaseTest {
  @Test
  void shouldCreateReadCompleteAndDeleteTask() throws Exception {
    String title = "API task " + UUID.randomUUID();
    APIResponse created = tasks.create(title);
    assertEquals(201, created.status());
    assertTrue(created.headers().get("content-type").contains("application/json"));
    Task task = tasks.read(created);
    try {
      assertFalse(task.id().isBlank());
      assertEquals(title, task.title());
      assertFalse(task.completed());
      APIResponse fetched = tasks.get(task.id());
      assertEquals(200, fetched.status());
      assertEquals(task, tasks.read(fetched));
      APIResponse updated = tasks.complete(task.id());
      assertEquals(200, updated.status());
      assertEquals(new Task(task.id(), title, true), tasks.read(updated));
      assertTrue(tasks.read(tasks.get(task.id())).completed());
      assertEquals(204, tasks.delete(task.id()).status());
      assertEquals(404, tasks.get(task.id()).status());
    } finally {
      tasks.delete(task.id());
    }
  }

  @ParameterizedTest(name = "Reject invalid title [{index}]")
  @ValueSource(strings = {"", " ", "   "})
  void shouldRejectBlankTitle(String title) {
    APIResponse response = tasks.create(title);
    assertEquals(400, response.status());
    assertTrue(response.text().contains("Title must contain"));
  }

  @Test
  void shouldRejectTitleAboveLimit() {
    assertEquals(400, tasks.create("x".repeat(121)).status());
  }

  @Test
  void shouldAcceptTitleAtLimit() throws Exception {
    APIResponse response = tasks.create("x".repeat(120));
    assertEquals(201, response.status());
    Task task = tasks.read(response);
    try {
      assertEquals(120, task.title().length());
    } finally {
      assertEquals(204, tasks.delete(task.id()).status());
    }
  }

  @Test
  void shouldReturn404ForUnknownTask() {
    APIResponse response = tasks.get(UUID.randomUUID().toString());
    assertEquals(404, response.status());
    assertTrue(response.text().contains("Task not found"));
  }

  @Test
  void shouldRejectMalformedJson() {
    APIResponse response =
        request.post(
            Endpoints.TASKS,
            RequestOptions.create()
                .setHeader("Content-Type", "application/json")
                .setData("{broken"));
    assertEquals(400, response.status());
    assertTrue(response.text().contains("Expected a JSON object"));
  }
}
