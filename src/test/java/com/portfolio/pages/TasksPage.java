package com.portfolio.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class TasksPage {
  private final Page page;

  public TasksPage(Page page) {
    this.page = page;
  }

  public TasksPage open() {
    page.navigate("./");
    return this;
  }

  public void addTask(String title) {
    page.getByLabel("Task title", new Page.GetByLabelOptions().setExact(true)).fill(title);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add task").setExact(true))
        .click();
  }

  public Locator task(String title) {
    return page.getByTestId("task")
        .filter(
            new Locator.FilterOptions()
                .setHas(page.getByText(title, new Page.GetByTextOptions().setExact(true))));
  }

  public void completeTask(String title) {
    task(title).getByRole(AriaRole.CHECKBOX).check();
  }

  public void deleteTask(String title) {
    task(title)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete"))
        .click();
  }

  public Locator error() {
    return page.getByRole(AriaRole.ALERT);
  }
}
