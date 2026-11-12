package com.mentormatching.user.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Cặp khoá RSA sinh ngẫu nhiên cho mỗi lần chạy test (không dùng key thật của auth-service)
 * và bộ ký token giống auth-service: RS256, iss, aud, sub = accountId, exp, roles.
 */
public final class TestJwtKeys {

    public static final String ISSUER = "auth-service";
    public static final String AUDIENCE = "mentor-matching-api";

    private static final TestJwtKeys TRUSTED = new TestJwtKeys(2048);

    private final KeyPair keyPair;
    private final NimbusJwtEncoder encoder;
    private Path publicKeyFile;

    private TestJwtKeys(int bits) {
        this.keyPair = generate(bits);
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    /** Khoá mà service under test tin cậy. */
    public static TestJwtKeys trusted() {
        return TRUSTED;
    }

    /** Một khoá khác, service không tin: dùng để kiểm tra chữ ký sai. */
    public static TestJwtKeys untrusted(int bits) {
        return new TestJwtKeys(bits);
    }

    /** Ghi public key dạng PEM (X.509 SubjectPublicKeyInfo) ra file tạm, trả URI {@code file:}. */
    public synchronized String publicKeyLocation() {
        if (publicKeyFile == null) {
            try {
                publicKeyFile = Files.createTempFile("user-test-jwt", ".pem");
                publicKeyFile.toFile().deleteOnExit();
                Files.writeString(publicKeyFile, pem(keyPair.getPublic().getEncoded()), StandardCharsets.US_ASCII);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return publicKeyFile.toUri().toString();
    }

    /** Token hợp lệ cho {@code userId} với các role cho trước. */
    public String token(UUID userId, String... roles) {
        return token(claims -> claims.subject(userId.toString()).claim("roles", List.of(roles)));
    }

    /** Token bắt đầu từ bộ claims hợp lệ, cho phép test sửa từng claim. */
    public String token(Consumer<JwtClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(15)))
                .claim("roles", List.of("LEANER"));
        customizer.accept(claims);
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private static KeyPair generate(int bits) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(bits);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String pem(byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----\n";
    }
}
