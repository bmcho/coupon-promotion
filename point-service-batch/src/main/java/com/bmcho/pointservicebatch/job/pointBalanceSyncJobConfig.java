package com.bmcho.pointservicebatch.job;

import com.bmcho.pointservicebatch.domain.DailyPointReport;
import com.bmcho.pointservicebatch.domain.DailyPointSummary;
import com.bmcho.pointservicebatch.domain.Point;
import com.bmcho.pointservicebatch.domain.PointBalance;
import com.bmcho.pointservicebatch.listener.JobCompletionNotificationListener;
import com.bmcho.pointservicebatch.repository.DailyPointReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.LocalDate.now;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class pointBalanceSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final RedissonClient redissonClient;
    private final DailyPointReportRepository dailyPointReportRepository;
    private final JobCompletionNotificationListener jobCompletionNotificationListener;

    @Bean
    @StepScope
    public EntityManager entityManager() {
        return entityManagerFactory.createEntityManager();
    }


    /*
     * 포인트 잔액 동기화 및 일별 리포트 생성 Job
     *
     * 실행 순서:
     * 1. syncPointBalanceStep: DB의 포인트 잔액을 Redis 캐시에 동기화
     * 2. generateDailyReportStep: 전일 포인트 트랜잭션을 집계하여 일별 리포트 생성
     */
    @Bean
    public Job pointBalanceSyncJob(
            Step syncPointBalanceRedisStep,
            Step generateDailyReportStep) {
        return new JobBuilder("pointBalanceSyncJob", jobRepository)
                .listener(jobCompletionNotificationListener)
                .start(syncPointBalanceRedisStep)
                .next(generateDailyReportStep)
                .build();
    }

    /**
     * 포인트 잔액 동기화 Step
     * <p>
     * DB의 포인트 잔액 정보를 Redis 캐시에 동기화하는 Step
     * - Reader: JPA를 통해 포인트 잔액 조회
     * - Processor: 캐시 키 생성
     * - Writer: Redis에 포인트 잔액 저장
     */
    @Bean
    public Step syncPointBalanceRedisStep(
            JpaPagingItemReader<PointBalance> pointBalanceRedisReader,
            ItemProcessor<PointBalance, Map.Entry<String, Long>> pointBalanceRedisProcessor,
            ItemWriter<Map.Entry<String, Long>> pointBalanceRedisWriter
    ) {
        return new StepBuilder("syncPointBalanceRedisStep", jobRepository)
                .<PointBalance, Map.Entry<String, Long>>chunk(1000, transactionManager)
                .reader(pointBalanceRedisReader)
                .processor(pointBalanceRedisProcessor)
                .writer(pointBalanceRedisWriter)
                .build();
    }


    /* 일별 리포트 생성 Step
     *
     * 전일 포인트 트랜잭션을 집계하여 일별 리포트를 생성하는 Step
     * - Reader: JPA를 통해 전일 포인트 트랜잭션 조회
     * - Processor: 포인트 트랜잭션을 사용자별로 집계
     * - Writer: 일별 리포트를 DB에 저장
     */
    @Bean
    public Step generateDailyReportStep(
            JpaPagingItemReader<Point> pointReader,
            ItemProcessor<Point, DailyPointSummary> pointToDailyReportProcessor,
            ItemWriter<DailyPointSummary> reportWriter) {
        return new StepBuilder("generateDailyReportStep", jobRepository)
                .<Point, DailyPointSummary>chunk(1000, transactionManager)
                .reader(pointReader)
                .processor(pointToDailyReportProcessor)
                .writer(reportWriter)
                .build();
    }

    /**
     * 포인트 잔액 Reader
     * <p>
     * JPA를 사용하여 DB에서 포인트 잔액 정보를 조회
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<PointBalance> pointBalanceRedisReader() {
        return new JpaPagingItemReaderBuilder<PointBalance>()
                .name("pointBalanceReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString("SELECT pb FROM PointBalance pb")
                .build();
    }

    /**
     * 포인트 잔액 Processor
     * <p>
     * 포인트 잔액을 Redis 캐시 키-값 쌍으로 변환
     */
    @Bean
    @StepScope
    public ItemProcessor<PointBalance, Map.Entry<String, Long>> pointBalanceRedisProcessor() {
        return pointBalance -> Map.entry(
                String.format("point:balance:%d", pointBalance.getUserId()),
                pointBalance.getBalance()
        );
    }

    /**
     * 포인트 잔액 Writer
     * <p>
     * Redis 캐시에 포인트 잔액 저장
     */
    @Bean
    @StepScope
    public ItemWriter<Map.Entry<String, Long>> pointBalanceRedisWriter() {
        return items -> {
            var balanceMap = redissonClient.getMap("point:balance");
            Map<String, Long> bulk = new HashMap<>(items.size());
            for (Map.Entry<String, Long> entry : items) {
                bulk.put(entry.getKey(), entry.getValue());
            }
            balanceMap.putAll(bulk);
        };
    }

    /**
     * 포인트 트랜잭션 Reader
     * <p>
     * 전일의 포인트 트랜잭션을 조회
     * 전일 범위를 "LocalDate reportDate"로 고정.
     * jobParameters['reportDate']가 없으면 기본값: 어제
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<Point> pointReader(
            @Value("#{jobParameters['reportDate']}") String reportDateStr
    ) {
        LocalDate reportDate = (reportDateStr ==  null)
                ? LocalDate.now().minusDays(1)
                : LocalDate.parse(reportDateStr);

        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = reportDate.plusDays(1).atStartOfDay();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startTime", start);
        parameters.put("endTime", end);

        return new JpaPagingItemReaderBuilder<Point>()
                .name("pointReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString("SELECT p FROM Point p WHERE p.createdAt BETWEEN :startTime AND :endTime")
                .parameterValues(parameters)
                .build();
    }

    /**
     * 포인트 트랜잭션 Processor
     * <p>
     * 포인트 트랜잭션을 사용자별로 집계하여 PointSummary 생성
     * SRP: Processor는 "변환"만 수행.
     * <p>
     * 주의: 지금 로직은 '집계'가 아니라 '건별 변환'입니다.
     * 진짜 사용자별 집계가 필요하면 이 구조만으론 부족(아래 설명 참고).
     */
    @Bean
    @StepScope
    public ItemProcessor<Point, DailyPointSummary> pointToDailyReportProcessor() {
        return point -> {
            long earned = 0L, used = 0L, canceled = 0L;

            switch (point.getType()) {
                case EARNED -> earned = point.getAmount();
                case USED -> used = point.getAmount();
                case CANCELED -> canceled = point.getAmount();
                default -> {
                    return null;
                }
            }
            return new DailyPointSummary(point.getUserId(), earned, used, canceled);
        };
    }

    /**
     * 일별 리포트 Writer
     * <p>
     * 집계된 포인트 트랜잭션을 일별 리포트로 변환하여 DB에 저장
     */
    @Bean
    @StepScope
    public ItemWriter<DailyPointSummary> reportWriter(EntityManager em) {
        return summaries -> {
            List<DailyPointReport> reports = new ArrayList<>();
            for (DailyPointSummary summary : summaries) {
                DailyPointReport report = DailyPointReport.builder()
                        .userId(summary.getUserId())
                        .reportDate(now().minusDays(1))  // 전일 데이터
                        .earnAmount(summary.getEarnAmount())
                        .useAmount(summary.getUseAmount())
                        .cancelAmount(summary.getCancelAmount())
                        .build();
                reports.add(report);
            }

            dailyPointReportRepository.saveAll(reports);
            dailyPointReportRepository.flush(); // JpaRepository면 가능
            em.clear();
        };
    }

}
