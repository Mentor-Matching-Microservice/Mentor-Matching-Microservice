package com.mentormatching.auth.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Chọn cách gửi email: SMTP khi {@code auth.mail.enabled=true}, ngược lại chỉ ghi log.
 * Bật SMTP mà thiếu cấu hình {@code spring.mail.host} thì dừng ngay khi khởi động.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccountMailProperties.class)
public class AccountMailConfiguration {

    @Bean
    @ConditionalOnMissingBean(AccountMailSender.class)
    AccountMailSender accountMailSender(AccountMailProperties properties, ObjectProvider<JavaMailSender> mailSender) {
        if (!properties.enabled()) {
            return new LoggingAccountMailSender(properties);
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("AUTH_MAIL_ENABLED=true requires SMTP settings (SPRING_MAIL_HOST, ...)");
        }
        return new SmtpAccountMailSender(sender, properties);
    }
}
