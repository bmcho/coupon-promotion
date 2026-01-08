package com.bmcho.timesaleservice.service.v3;

import com.bmcho.timesaleservice.domain.TimeSaleOrderStatus;
import com.bmcho.timesaleservice.dto.PurchaseRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 타임세일 구매 요청을 처리하는 Producer
 * - Kafka를 통해 비동기로 구매 요청을 처리
 * - Redis를 사용하여 대기열 관리
 * - Redisson을 사용하여 분산 환경에서의 동시성 제어
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSaleProducer {
    // Kafka 토픽 이름
    private static final String TOPIC = "time-sale-requests";
    // Redis 키 접두사
    private static final String QUEUE_KEY = "time-sale-queue:";
    private static final String QUEUE_SEQ_KEY = "time-sale-queue-seq:";
    private static final String TOTAL_REQUESTS_KEY = "time-sale-total-requests:";
    private static final String RESULT_PREFIX = "purchase-result:";

    private final KafkaTemplate<String, PurchaseRequestMessage> kafkaTemplate;
    private final RedissonClient redissonClient;

    /**
     * 타임세일 구매 요청을 처리
     * 1. 요청 ID 생성
     * 2. Redis에 요청 상태 저장
     * 3. 대기열에 요청 추가
     * 4. Kafka로 메시지 전송
     */
    public String sendPurchaseRequest(Long timeSaleId, Long userId, Long quantity) {
        //고유 ID 생성
        String requestId = UUID.randomUUID().toString();

        // 구매 요청 메시지 생성
        PurchaseRequestMessage message = PurchaseRequestMessage.builder()
                .requestId(requestId)
                .timeSaleId(timeSaleId)
                .userId(userId)
                .quantity(quantity)
                .build();

        // Redis에 초기 상태 저장
        RBucket<TimeSaleOrderStatus> resultBucket = redissonClient.getBucket(RESULT_PREFIX + requestId);
        resultBucket.set(TimeSaleOrderStatus.PENDING);

        // 대기열에 추가하고 카운터 증가
        String queueKey = QUEUE_KEY + timeSaleId;
        String queueSeqKey = QUEUE_SEQ_KEY + timeSaleId;
        String totalKey = TOTAL_REQUESTS_KEY + timeSaleId;

        RAtomicLong queueSeq = redissonClient.getAtomicLong(queueSeqKey);
        long incremented = queueSeq.incrementAndGet();
        RScoredSortedSet<String> queueBucket = redissonClient.getScoredSortedSet(queueKey);
        queueBucket.add(incremented, requestId);

        RAtomicLong totalCounter = redissonClient.getAtomicLong(totalKey);
        totalCounter.incrementAndGet();

        // Kafka로 메시지 전송
        kafkaTemplate.send(TOPIC, requestId, message);
        return requestId;

    }

    /**
     * 대기열에서 요청의 위치를 조회
     */
    public Integer getQueuePosition(Long timeSaleId, String requestId) {
        String queueKey = QUEUE_KEY + timeSaleId;
        RScoredSortedSet<String> queueBucket = redissonClient.getScoredSortedSet(queueKey);

        if (!queueBucket.contains(requestId) || queueBucket.isEmpty()) {
            return null;
        }

        return queueBucket.rank(requestId);
    }

    /**
     * 총 대기 중인 요청 수를
     */
    public Long getTotalWaiting(Long timeSaleId) {
        String totalKey = TOTAL_REQUESTS_KEY + timeSaleId;
        RAtomicLong totalCounter = redissonClient.getAtomicLong(totalKey);
        return totalCounter.get();
    }
}
