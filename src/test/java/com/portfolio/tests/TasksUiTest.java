package com.portfolio.tests;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIResponse;
import com.portfolio.api.Endpoints;
import com.portfolio.model.Task;
import com.portfolio.pages.TasksPage;
import com.portfolio.support.UiTest;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class TasksUiTest extends UiTest {
  @Test
  void shouldCreateTaskInUiAndPersistAfterReload() throws Exception {
    TasksPage board = new TasksPage(page).open();
    String title = "UI task " + UUID.randomUUID();
    var created =
        page.waitForResponse(
            response ->
                response.url().endsWith("/" + Endpoints.TASKS)
                    && response.request().method().equals("POST"),
            () -> board.addTask(title));
    assertEquals(201, created.status());
    Task task =
        new com.fasterxml.jackson.databind.ObjectMapper().readValue(created.text(), Task.class);
    try {
      assertThat(board.task(title)).isVisible();
      assertEquals(title, tasks.read(tasks.get(task.id())).title());
      page.reload();
      assertThat(board.task(title)).isVisible();
    } finally {
      tasks.delete(task.id());
    }
  }

  @Test
  void shouldCompleteApiCreatedTaskInUi() throws Exception {
    APIResponse created = tasks.create("Complete task " + UUID.randomUUID());
    assertEquals(201, created.status());
    Task task = tasks.read(created);
    try {
      TasksPage board = new TasksPage(page).open();
      board.completeTask(task.title());
      assertThat(board.task(task.title())).hasAttribute("data-completed", "true");
      assertTrue(tasks.read(tasks.get(task.id())).completed());
      page.reload();
      assertThat(board.task(task.title())).hasAttribute("data-completed", "true");
    } finally {
      tasks.delete(task.id());
    }
  }

  @Test
  void shouldDeleteApiCreatedTaskInUi() throws Exception {
    APIResponse created = tasks.create("Delete task " + UUID.randomUUID());
    assertEquals(201, created.status());
    Task task = tasks.read(created);
    try {
      TasksPage board = new TasksPage(page).open();
      assertThat(board.task(task.title())).isVisible();
      board.deleteTask(task.title());
      assertThat(board.task(task.title())).hasCount(0);
      assertEquals(404, tasks.get(task.id()).status());
    } finally {
      tasks.delete(task.id());
    }
  }

  @Test
  void shouldShowValidationErrorForEmptyTitle() {
    TasksPage board = new TasksPage(page).open();
    board.addTask("");
    assertThat(board.error()).hasText("Title must contain 1–120 characters");
  }
}
