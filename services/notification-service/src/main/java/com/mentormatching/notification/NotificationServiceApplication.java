package com.mentormatching.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification service: hộp thư thông báo của người dùng (MongoDB {@code mm_notification})
 * và kênh đẩy realtime qua STOMP WebSocket.
 */
@SpringBootApplication
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}
