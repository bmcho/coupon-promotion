package com.bmcho.timesaleservice.service.v3;

import com.bmcho.timesaleservice.domain.TimeSaleOrderStatus;
import com.bmcho.timesaleservice.domain.TimeSaleStatus;
import com.bmcho.timesaleservice.dto.PurchaseRequestMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSaleProducerTest {

    @Mock
    private KafkaTemplate<String, PurchaseRequestMessage> kafkaTemplate;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RBucket<TimeSaleOrderStatus> resultBucket;
    @Mock
    private RScoredSortedSet<String> queueBucket;
    @Mock
    private RAtomicLong queueSeq;
    @Mock
    private RAtomicLong totalCounter;
    @InjectMocks
    private TimeSaleProducer timeSaleProducer;

//    private final String TOPIC = "time-sale-requests";
//    // Redis 키 접두사
//    private final String QUEUE_KEY = "time-sale-queue:";
//    private final String QUEUE_SEQ_KEY = "time-sale-queue-seq:";
//    private final String TOTAL_REQUESTS_KEY = "time-sale-total-requests:";
//    private final String RESULT_PREFIX = "purchase-result:";


    @Test
    @DisplayName("구매 요청 전송 성공")
    void sendPurchaseRequest_Success() {
        //given
        Long timeSaleId = 1L;
        Long userId = 1L;
        Long quantity = 2L;
        when(redissonClient.<TimeSaleOrderStatus>getBucket(matches("purchase-result:.*"))).thenReturn(resultBucket);
        when(redissonClient.<String>getScoredSortedSet(matches("time-sale-queue:.*"))).thenReturn(queueBucket);
        when(redissonClient.getAtomicLong(matches("time-sale-total-requests:.*"))).thenReturn(totalCounter);
        when(redissonClient.getAtomicLong(matches("time-sale-queue-seq:.*"))).thenReturn(queueSeq);

        //when
        String requestId = timeSaleProducer.sendPurchaseRequest(timeSaleId, userId, quantity);

        //then
        verify(resultBucket).set(TimeSaleOrderStatus.PENDING);
        verify(queueBucket).add(queueSeq.get(), requestId);
        verify(totalCounter).incrementAndGet();
        verify(queueSeq).incrementAndGet();
        verify(kafkaTemplate).send(eq("time-sale-requests"), eq(requestId), any(PurchaseRequestMessage.class));
        assertThat(requestId).isNotNull();
    }

    @Test
    @DisplayName("대기열 위치 조회 성공")
    void getQueuePosition_Success() {
        Long timeSaleId = 1L;
        String requestId = "requestId";
        //given
        when(redissonClient.<String>getScoredSortedSet("time-sale-queue:%d".formatted(timeSaleId))).thenReturn(queueBucket);
        when(queueBucket.contains(anyString())).thenReturn(true);
        when(queueBucket.isEmpty()).thenReturn(false);
        when(queueBucket.rank(requestId)).thenReturn(1);

        //when
        Integer result = timeSaleProducer.getQueuePosition(timeSaleId, requestId);

        //then
        assertThat(result).isEqualTo(1);
    }

    @Test
    void getTotalWaiting_Success() {
        //given
        Long timeSaleId = 1L;
        Long totalCount = 20L;
        String timeSaleTotalRequestsKey = "time-sale-total-requests:%d";
        given(redissonClient.getAtomicLong(timeSaleTotalRequestsKey.formatted(timeSaleId)))
                .willReturn(totalCounter);
        when(totalCounter.get()).thenReturn(totalCount);

        //when
        Long result = timeSaleProducer.getTotalWaiting(timeSaleId);

        //then
        assertThat(result).isEqualTo(totalCount);
    }
}