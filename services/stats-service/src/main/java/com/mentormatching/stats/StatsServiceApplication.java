package com.mentormatching.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Thống kê cho Admin Dashboard (#82), tổng hợp từ sự kiện nghiệp vụ trên RabbitMQ vào MongoDB {@code mm_stats}.
 * Không đọc database của service khác và không gọi service khác khi truy vấn.
 */
@SpringBootApplication
public class StatsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StatsServiceApplication.class, args);
	}

}
