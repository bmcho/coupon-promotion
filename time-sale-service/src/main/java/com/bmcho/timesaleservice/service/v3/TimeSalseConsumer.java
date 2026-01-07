package com.bmcho.timesaleservice.service.v3;

import com.bmcho.timesaleservice.domain.TimeSale;
import com.bmcho.timesaleservice.domain.TimeSaleOrder;
import com.bmcho.timesaleservice.domain.TimeSaleOrderStatus;
import com.bmcho.timesaleservice.dto.PurchaseRequestMessage;
import com.bmcho.timesaleservice.exception.TimeSaleException;
import com.bmcho.timesaleservice.repository.TimeSaleOrderRepository;
import com.bmcho.timesaleservice.repository.TimeSaleRepository;
import com.bmcho.timesaleservice.service.v2.TimeSaleRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 타임세일 구매 요청을 처리하는 Consumer
 * - Kafka를 통해 비동기로 전달된 구매 요청을 처리
 * - Redis의 재고를 감소시키고 주문을 생성
 * - 대기열에서 처리된 요청을 제거
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSalseConsumer {

    private final TimeSaleRedisService timeSaleRedisService;
    private final TimeSaleOrderRepository timeSaleOrderRepository;
    private final TimeSaleRepository timeSaleRepository;
    private final RedissonClient redissonClient;

    // Redis 키 접두사
    private static final String RESULT_PREFIX = "purchase-result:";
    private static final String QUEUE_KEY = "time-sale-queue:";
    private static final String TOTAL_REQUESTS_KEY = "time-sale-total-requests:";

    /**
     * Kafka로부터 수신한 구매 요청을 처리
     * 1. Redis에서 타임세일 정보 조회
     * 2. 재고 감소
     * 3. 주문 생성
     * 4. 결과 저장
     * 5. 대기열에서 제거
     *
     * @param message 구매 요청 메시지
     */
    @Transactional
    @KafkaListener(topics = "time-sale-requests", groupId = "time-sale-group")
    public void consumePurchaseRequest(PurchaseRequestMessage message) {
        try {
            // 타임세일 정보 조회 및 수량 확인
            TimeSale timeSale = timeSaleRepository.findById(message.getTimeSaleId())
                    .orElseThrow(() -> TimeSaleException.notFound(message.getTimeSaleId()));
            timeSale.purchase(message.getQuantity());

            // 변경 사항 저장
            timeSaleRepository.save(timeSale);

            // 주문정보 생성 및 저장
            TimeSaleOrder order = TimeSaleOrder.builder()
                    .userId(message.getUserId())
                    .timeSale(timeSale)
                    .quantity(message.getQuantity())
                    .discountPrice(timeSale.getDiscountPrice())
                    .build();

            TimeSaleOrder savedOrder = timeSaleOrderRepository.save(order);
            savedOrder.complete();

            // Redis 대기열에 주문 상태 성공으로 변경
            savePurchaseResult(message.getRequestId(), TimeSaleOrderStatus.SUCCESS);
        } catch (Exception e) {
            log.error("Failed to process purchase request: {}", message, e);
            // 실패 결과 저장
            savePurchaseResult(message.getRequestId(), TimeSaleOrderStatus.FAIL);
        } finally {
            // 대기열에서 제거
            removeFromQueue(message.getTimeSaleId(), message.getRequestId());
        }
    }

    /**
     * 구매 요청의 처리 결과를 Redis에 저장
     *
     * @param requestId 요청 ID
     * @param result    처리 결과 (SUCCESS/FAIL)
     */
    private void savePurchaseResult(String requestId, TimeSaleOrderStatus result) {
        RBucket<TimeSaleOrderStatus> resultBucket = redissonClient.getBucket(RESULT_PREFIX + requestId);
        resultBucket.set(result);
    }

    /**
     * 대기열에서 처리 완료된 요청을 제거
     * 1. 대기열에서 요청 ID 제거
     * 2. 총 대기 수 감소
     *
     * @param timeSaleId 타임세일 ID
     * @param requestId  요청 ID
     */
    private void removeFromQueue(Long timeSaleId, String requestId) {
        try {
            //대기열 제거
            String queueKey = QUEUE_KEY + timeSaleId;
            RScoredSortedSet<String> queueBucket = redissonClient.getScoredSortedSet(queueKey);

            if(queueBucket.contains(requestId)) {
                queueBucket.remove(requestId);
            }

            // 총 대기 수 감소
            String totalKey = TOTAL_REQUESTS_KEY + timeSaleId;
            RAtomicLong totalCounter = redissonClient.getAtomicLong(totalKey);
            totalCounter.decrementAndGet();

        } catch (Exception e) {
            log.error("Failed to remove request from queue: timeSaleId={}, requestId={}", timeSaleId, requestId, e);
        }
    }


}
