package com.bmcho.pointservicebatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PointServiceBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointServiceBatchApplication.class, args);
    }

}
