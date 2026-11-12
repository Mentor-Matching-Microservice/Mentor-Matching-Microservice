package com.mentormatching.auth.controller;

import java.time.Duration;
import java.util.Map;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Công bố public key ký access token theo chuẩn JWKS (RFC 7517) để các resource service giải mã/kiểm tra JWT
 * mà không cần file public.pem. Chỉ trả phần công khai ({@code kty, e, n, kid, use, alg}); private key không
 * bao giờ rời khỏi auth-service. Cho phép cache ngắn để khi xoay khóa các service nhận khóa mới nhanh.
 */
@RestController
public class JwksController {

    public static final String PATH = "/.well-known/jwks.json";
    static final Duration MAX_AGE = Duration.ofMinutes(5);

    private final Map<String, Object> publicJwks;

    public JwksController(RSAKey jwtSigningKey) {
        RSAKey publicOnly = jwtSigningKey.toPublicJWK();
        if (publicOnly.isPrivate()) {
            throw new IllegalStateException("JWKS must only contain public key material");
        }
        this.publicJwks = new JWKSet(publicOnly).toJSONObject(true);
    }

    @GetMapping(value = PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(MAX_AGE).cachePublic()).body(publicJwks);
    }
}
