package com.jellystudy;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableDubbo
@EnableJpaRepositories(basePackages = "com.jellystudy.repository.mysql")
@EnableMongoRepositories(basePackages = "com.jellystudy.repository.mongo")
public class StudyPlanApplication {
    public static void main(String[] args) {
        String nacosLogPath = System.getProperty("user.dir") + "/logs/nacos";
        System.setProperty("nacos.logging.path", nacosLogPath);
        System.setProperty("dubbo.metadata-report.address", "nacos://localhost:8848");
        SpringApplication.run(StudyPlanApplication.class, args);
    }
}
