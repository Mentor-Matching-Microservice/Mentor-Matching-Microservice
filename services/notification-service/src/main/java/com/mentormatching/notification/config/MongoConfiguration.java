package com.mentormatching.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Bật transaction MongoDB (yêu cầu replica set). Các bước sau (#73, #81) cần ghi
 * bản ghi chống trùng {@code inbox} và thông báo trong cùng một transaction.
 * Spring Boot không tự tạo transaction manager cho MongoDB nên khai báo tại đây.
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class MongoConfiguration {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
