package com.portfolio.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.portfolio.model.Task;
import java.util.Map;

public final class TasksClient {
  private final APIRequestContext request;
  private final ObjectMapper json = new ObjectMapper();

  public TasksClient(APIRequestContext request) {
    this.request = request;
  }

  public APIResponse create(String title) {
    return request.post(Endpoints.TASKS, RequestOptions.create().setData(Map.of("title", title)));
  }

  public APIResponse get(String id) {
    return request.get(Endpoints.task(id));
  }

  public APIResponse complete(String id) {
    return request.patch(
        Endpoints.task(id), RequestOptions.create().setData(Map.of("completed", true)));
  }

  public APIResponse delete(String id) {
    return request.delete(Endpoints.task(id));
  }

  public Task read(APIResponse response) throws JsonProcessingException {
    return json.readValue(response.text(), Task.class);
  }
}
