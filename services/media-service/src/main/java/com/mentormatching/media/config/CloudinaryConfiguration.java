package com.mentormatching.media.config;

import java.util.Map;
import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfiguration {
    @Bean
    Cloudinary cloudinary(MediaProperties properties) {
        return new Cloudinary(Map.of(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret(),
                "secure", true));
    }
}
