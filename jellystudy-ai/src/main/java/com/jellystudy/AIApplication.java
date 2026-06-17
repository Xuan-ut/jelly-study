package com.jellystudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AIApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.path", System.getProperty("user.dir") + "/logs/nacos");
        System.setProperty("dubbo.metadata-report.address", "nacos://localhost:8848");
        SpringApplication.run(AIApplication.class, args);
    }
}
