package com.jellystudy.companion;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableDubbo
@EnableMongoRepositories(basePackages = "com.jellystudy.companion.repository")
public class CompanionApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.path", System.getProperty("user.dir") + "/logs/nacos");
        System.setProperty("dubbo.metadata-report.address", "nacos://localhost:8848");
        SpringApplication.run(CompanionApplication.class, args);
    }
}
