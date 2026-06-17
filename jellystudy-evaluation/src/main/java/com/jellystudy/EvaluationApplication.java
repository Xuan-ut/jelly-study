package com.jellystudy;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class EvaluationApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.path", System.getProperty("user.dir") + "/logs/nacos");
        System.setProperty("dubbo.metadata-report.address", "nacos://localhost:8848");
        SpringApplication.run(EvaluationApplication.class, args);
    }
}
