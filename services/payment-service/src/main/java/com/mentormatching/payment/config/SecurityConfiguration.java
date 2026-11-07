package com.mentormatching.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception{
        JwtGrantedAuthoritiesConverter roles=new JwtGrantedAuthoritiesConverter();roles.setAuthoritiesClaimName("roles");roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter=new JwtAuthenticationConverter();converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(AbstractHttpConfigurer::disable).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.POST,"/api/v1/payments/stripe/webhook").permitAll()
                        .requestMatchers("/actuator/health","/actuator/health/**").permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
    }
}
