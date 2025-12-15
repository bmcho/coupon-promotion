package com.bmcho.pointservicebatch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDate;

@Configuration
@EnableBatchProcessing
@EnableTransactionManagement
@RequiredArgsConstructor
public class BatchConfig {

    private final JobLauncher jobLauncher;
    private final Job pointBalanceSyncJob;

    @Bean
    public ApplicationRunner pointBalanceSyncJobRunner() {
        return args -> {
            jobLauncher.run(
                    pointBalanceSyncJob,
                    new JobParametersBuilder()
                            .addString("reportDate", LocalDate.now().minusDays(1).toString())
                            .addLong("timestamp", System.currentTimeMillis())
                            .toJobParameters()
            );
        };
    }

}
