package com.mentormatching.user.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.mentormatching.user.dto.response.UserStatusResult;
import com.mentormatching.user.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Gọi API admin có sẵn của auth-service ({@code PATCH /api/v1/admin/accounts/{id}/status}, #64) — auth-service là
 * nơi sở hữu trạng thái tài khoản. Chuyển tiếp nguyên access token của admin, nên auth-service tự kiểm tra quyền ADMIN.
 * <ul>
 *   <li>4xx của auth: trả về cùng status và mã lỗi (vd. 404 RESOURCE_NOT_FOUND, 409 CONFLICT).</li>
 *   <li>auth không phản hồi / 5xx: 503 SERVICE_UNAVAILABLE (không đổi gì phía user-service).</li>
 * </ul>
 */
@Component
public class AuthAccountClient {

    private final RestClient rest;
    private final ObjectMapper objectMapper;

    public AuthAccountClient(@Value("${user.auth.base-url:http://localhost:8081}") String baseUrl,
                             @Value("${user.auth.timeout:5s}") Duration timeout, ObjectMapper objectMapper) {
        // JDK HttpClient: HttpURLConnection (SimpleClientHttpRequestFactory) không hỗ trợ PATCH.
        HttpClient http = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.objectMapper = objectMapper;
    }

    public UserStatusResult changeStatus(String adminBearerToken, UUID accountId, String status, String reason) {
        String body;
        try {
            body = rest.patch().uri("/api/v1/admin/accounts/{id}/status", accountId)
                    .header("Authorization", "Bearer " + adminBearerToken)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(reason == null ? Map.of("status", status) : Map.of("status", status, "reason", reason))
                    .retrieve().body(String.class);
        } catch (HttpClientErrorException ex) {
            throw new ApiException(HttpStatus.valueOf(ex.getStatusCode().value()), codeOf(ex), messageFor(ex.getStatusCode()));
        } catch (RestClientException ex) {
            throw new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, ex);
        }
        JsonNode data = objectMapper.readTree(body).get("data");
        return new UserStatusResult(UUID.fromString(data.get("accountId").asString()), data.get("status").asString(),
                data.get("aggregateVersion").asLong());
    }

    private String codeOf(HttpClientErrorException ex) {
        try {
            JsonNode code = objectMapper.readTree(ex.getResponseBodyAsString()).get("code");
            if (code != null && code.isString() && code.asString().matches("[A-Z][A-Z0-9_]*")) {
                return code.asString();
            }
        } catch (RuntimeException ignored) {
            // body không phải ProblemDetail
        }
        return "REQUEST_REJECTED";
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "The status change was rejected.";
            case 401 -> "Authentication is required to access this resource.";
            case 403 -> "You do not have permission to perform this request.";
            case 404 -> "Account not found.";
            case 409 -> "The status change conflicts with the account state.";
            default -> "The request could not be processed.";
        };
    }
}
