package com.jellystudy;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableDubbo
@EnableMongoRepositories(basePackages = "com.jellystudy.repository")
public class KnowledgeApplication {
    public static void main(String[] args) {
        String nacosLogPath = System.getProperty("user.dir") + "/logs/nacos";
        System.setProperty("nacos.logging.path", nacosLogPath);
        System.setProperty("dubbo.metadata-report.address", "nacos://localhost:8848");
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}
