package com.mentormatching.mentor.config;

import java.net.http.HttpClient;
import java.time.Duration;

import com.mentormatching.mentor.client.MediaServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class MediaConfiguration {

    @Bean
    public MediaServiceClient mediaServiceClient(
            @Value("${mentor.media-service.base-url:http://localhost:8087}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
        factory.setReadTimeout(Duration.ofSeconds(5));

        return new MediaServiceClient(RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build());
    }
}
