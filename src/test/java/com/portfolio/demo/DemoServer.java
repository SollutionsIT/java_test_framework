package com.portfolio.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.model.Task;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** A local system under test; deliberately independent of the test client's endpoint constants. */
public final class DemoServer implements AutoCloseable {
  private final HttpServer server;
  private final ObjectMapper json = new ObjectMapper();
  private final Map<String, Task> tasks = new LinkedHashMap<>();
  private final String root;
  private final String api;

  public DemoServer(String baseUrl) throws IOException {
    URI uri = URI.create(baseUrl);
    if (!"http".equals(uri.getScheme())
        || !"127.0.0.1".equals(uri.getHost())
        || uri.getPort() < 1) {
      throw new IllegalArgumentException(
          "DEMO_MODE requires http://127.0.0.1:<port>/[prefix/]. For a remote server set DEMO_MODE=false.");
    }
    root = uri.getPath();
    api = root + "api/tasks";
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", uri.getPort()), 0);
    server.createContext("/", this::handle);
  }

  public void start() {
    server.start();
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      String path = exchange.getRequestURI().getPath();
      String method = exchange.getRequestMethod();
      if (path.equals(root) && method.equals("GET")) {
        try (var resource = DemoServer.class.getResourceAsStream("/demo/index.html")) {
          if (resource == null) {
            throw new IllegalStateException("Missing demo/index.html");
          }
          byte[] html = resource.readAllBytes();
          exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
          exchange.sendResponseHeaders(200, html.length);
          exchange.getResponseBody().write(html);
        }
      } else if (path.equals(api)) {
        collection(exchange, method);
      } else if (path.startsWith(api + "/")) {
        item(exchange, method, path.substring(api.length() + 1));
      } else {
        send(exchange, 404, Map.of("error", "Not found"));
      }
    }
  }

  private void collection(HttpExchange exchange, String method) throws IOException {
    switch (method) {
      case "GET" -> send(exchange, 200, tasks.values());
      case "POST" -> {
        JsonNode body = readBody(exchange);
        if (body == null) {
          return;
        }
        JsonNode title = body.path("title");
        if (!title.isTextual()
            || title.asText().isBlank()
            || title.asText().strip().length() > 120) {
          send(exchange, 400, Map.of("error", "Title must contain 1–120 characters"));
          return;
        }
        Task task = new Task(UUID.randomUUID().toString(), title.asText().strip(), false);
        tasks.put(task.id(), task);
        send(exchange, 201, task);
      }
      default -> send(exchange, 405, Map.of("error", "Method not allowed"));
    }
  }

  private void item(HttpExchange exchange, String method, String id) throws IOException {
    Task task = tasks.get(id);
    if (task == null) {
      send(exchange, 404, Map.of("error", "Task not found"));
      return;
    }
    switch (method) {
      case "GET" -> send(exchange, 200, task);
      case "PATCH" -> {
        JsonNode body = readBody(exchange);
        if (body == null) {
          return;
        }
        if (!body.path("completed").isBoolean()) {
          send(exchange, 400, Map.of("error", "completed must be boolean"));
          return;
        }
        Task updated = new Task(task.id(), task.title(), body.get("completed").asBoolean());
        tasks.put(id, updated);
        send(exchange, 200, updated);
      }
      case "DELETE" -> {
        tasks.remove(id);
        exchange.sendResponseHeaders(204, -1);
      }
      default -> send(exchange, 405, Map.of("error", "Method not allowed"));
    }
  }

  private JsonNode readBody(HttpExchange exchange) throws IOException {
    try {
      JsonNode body = json.readTree(exchange.getRequestBody());
      if (body != null && body.isObject()) {
        return body;
      }
    } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
      // A malformed JSON request is a client error, not an unhandled server exception.
    }
    send(exchange, 400, Map.of("error", "Expected a JSON object"));
    return null;
  }

  private void send(HttpExchange exchange, int status, Object body) throws IOException {
    byte[] bytes = json.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
