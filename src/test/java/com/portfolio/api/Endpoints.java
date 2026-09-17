package com.portfolio.api;

public final class Endpoints {
  // Relative paths preserve an optional BASE_URL prefix, e.g. https://host/qa/.
  public static final String TASKS = "api/tasks";

  private Endpoints() {}

  public static String task(String id) {
    return TASKS + "/" + id;
  }
}
