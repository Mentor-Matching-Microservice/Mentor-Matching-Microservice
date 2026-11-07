package com.mentormatching.booking.config;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
@Configuration
public class JwtConfiguration {
    @Bean JwtDecoder jwtDecoder(@Value("${booking.jwt.public-key-location}") Resource resource,@Value("${booking.jwt.issuer}") String issuer,@Value("${booking.jwt.audience}") String audience)throws Exception{
        String pem=new String(resource.getInputStream().readAllBytes()).replaceAll("-----[^-]+-----","").replaceAll("\\s","");
        RSAPublicKey key=(RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withPublicKey(key).build();
        OAuth2TokenValidator<Jwt> aud=jwt->jwt.getAudience()!=null&&jwt.getAudience().contains(audience)?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","JWT audience is invalid",null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),aud));return decoder;
    }
}
