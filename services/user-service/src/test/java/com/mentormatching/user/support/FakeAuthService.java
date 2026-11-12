package com.mentormatching.user.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Giả lập {@code PATCH /api/v1/admin/accounts/{id}/status} của auth-service (#64) với đúng hình dạng response
 * ({@code ApiResponse<AccountStatusResult>} / ProblemDetail). Ghi lại header Authorization nhận được.
 */
public final class FakeAuthService {

    private static final Pattern PATH = Pattern.compile("/api/v1/admin/accounts/([0-9a-f-]{36})/status");

    private final HttpServer server;
    private final Map<String, Integer> failures = new ConcurrentHashMap<>();
    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private volatile long version = 10;

    public FakeAuthService() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** Auth trả lỗi HTTP này cho tài khoản (404, 409, 500...). */
    public void failWith(String accountId, int status) {
        failures.put(accountId, status);
    }

    public List<String> authorizations() {
        return authorizations;
    }

    private void handle(HttpExchange exchange) throws IOException {
        Matcher matcher = PATH.matcher(exchange.getRequestURI().getPath());
        if (!"PATCH".equals(exchange.getRequestMethod()) || !matcher.matches()) {
            respond(exchange, 404, "{\"status\":404,\"code\":\"RESOURCE_NOT_FOUND\"}");
            return;
        }
        authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
        String accountId = matcher.group(1);
        Integer failure = failures.get(accountId);
        if (failure != null) {
            String code = switch (failure) {
                case 404 -> "RESOURCE_NOT_FOUND";
                case 409 -> "CONFLICT";
                default -> "INTERNAL_SERVER_ERROR";
            };
            respond(exchange, failure, "{\"status\":" + failure + ",\"code\":\"" + code + "\"}");
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String status = body.replaceAll(".*\"status\"\\s*:\\s*\"([A-Z_]+)\".*", "$1");
        long newVersion = ++version;
        respond(exchange, 200, """
                {"status":200,"code":"SUCCESS","success":true,"message":"Account status updated","data":{
                  "accountId":"%s","status":"%s","aggregateVersion":%d,"revokedRefreshTokens":1}}
                """.formatted(accountId, status, newVersion));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", status >= 400 ? "application/problem+json" : "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
