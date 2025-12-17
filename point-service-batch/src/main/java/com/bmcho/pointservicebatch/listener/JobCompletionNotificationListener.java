package com.bmcho.pointservicebatch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {

    private final ConfigurableApplicationContext context;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job {} is starting...", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job {} completed successfully", jobExecution.getJobInstance().getJobName());
        } else {
            log.error("Job {} failed with status {}", 
                jobExecution.getJobInstance().getJobName(), 
                jobExecution.getStatus());
        }
        int code = SpringApplication.exit(context, () -> jobExecution.getExitStatus().getExitCode().equals("COMPLETED") ? 0 : 1);
        System.exit(code);
    }
}