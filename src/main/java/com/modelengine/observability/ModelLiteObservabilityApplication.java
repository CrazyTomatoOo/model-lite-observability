package com.modelengine.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class ModelLiteObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModelLiteObservabilityApplication.class, args);
    }
}
