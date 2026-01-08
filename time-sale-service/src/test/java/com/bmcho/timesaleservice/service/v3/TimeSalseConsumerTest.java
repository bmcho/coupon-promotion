package com.bmcho.timesaleservice.service.v3;

import com.bmcho.timesaleservice.domain.*;
import com.bmcho.timesaleservice.dto.PurchaseRequestMessage;
import com.bmcho.timesaleservice.repository.TimeSaleOrderRepository;
import com.bmcho.timesaleservice.repository.TimeSaleRepository;
import com.bmcho.timesaleservice.service.v2.TimeSaleRedisService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimeSalseConsumerTest {

    @Mock
    private TimeSaleRedisService timeSaleRedisService;
    @Mock
    private TimeSaleOrderRepository timeSaleOrderRepository;
    @Mock
    private TimeSaleRepository timeSaleRepository;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RBucket<TimeSaleOrderStatus> resultBucket;
    @Mock
    private RScoredSortedSet<String> queueBucket;
    @Mock
    private RAtomicLong totalCounter;

    @InjectMocks
    private TimeSaleConsumer timeSaleConsumer;

    private TimeSale timeSale;
    private TimeSaleOrder order;
    private Product product;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(10000L)
                .build();

        timeSale = TimeSale.builder()
                .id(1L)
                .product(product)
                .status(TimeSaleStatus.ACTIVE)
                .quantity(100L)
                .remainingQuantity(100L)
                .discountPrice(5000L)
                .startAt(now.minusHours(1))
                .endAt(now.plusHours(1))
                .build();

        order = TimeSaleOrder.builder()
                .id(1L)
                .userId(1L)
                .timeSale(timeSale)
                .quantity(2L)
                .discountPrice(5000L)
                .build();

    }

//    // Redis 키 접두사
    private static final String RESULT_PREFIX = "purchase-result:";
    private static final String QUEUE_KEY = "time-sale-queue:";
    private static final String TOTAL_REQUESTS_KEY = "time-sale-total-requests:";

    @Test
    @DisplayName("구매 요청 처리 성공")
    void consumePurchaseRequest_Success() {
        //given
        PurchaseRequestMessage message = PurchaseRequestMessage.builder()
                .requestId("test-request-id")
                .timeSaleId(1L)
                .userId(1L)
                .quantity(2L)
                .build();

        String resultKey = RESULT_PREFIX + message.getRequestId();
        String queueKey = QUEUE_KEY + message.getTimeSaleId();
        String totalKey = TOTAL_REQUESTS_KEY + message.getTimeSaleId();

        given(timeSaleRepository.findById(message.getTimeSaleId()))
                .willReturn(Optional.of(timeSale));
        given(timeSaleRepository.save(any(TimeSale.class)))
                .willReturn(timeSale);
        given(timeSaleOrderRepository.save(any(TimeSaleOrder.class)))
                .willReturn(order);
        given(redissonClient.<TimeSaleOrderStatus>getBucket(resultKey))
                .willReturn(resultBucket);
        given(redissonClient.<String>getScoredSortedSet(queueKey))
                .willReturn(queueBucket);
        given(redissonClient.getAtomicLong(totalKey))
                .willReturn(totalCounter);

        //when
        timeSaleConsumer.consumePurchaseRequest(message);

        //then
        verify(timeSaleRedisService).saveToRedis(timeSale);
        verify(resultBucket).set(TimeSaleOrderStatus.SUCCESS);
        verify(totalCounter).decrementAndGet();
    }

    @Test
    @DisplayName("구매 요청 처리 실패 - 타임세일 없음")
    void consumePurchaseRequest_TimeSaleNotFound() {
        // given
        PurchaseRequestMessage message = PurchaseRequestMessage.builder()
                .requestId("test-request-id")
                .timeSaleId(1L)
                .userId(1L)
                .quantity(2L)
                .build();

        String resultKey = RESULT_PREFIX + message.getRequestId();
        String queueKey = QUEUE_KEY + message.getTimeSaleId();
        String totalKey = TOTAL_REQUESTS_KEY + message.getTimeSaleId();

        given(timeSaleRepository.findById(1L)).willReturn(Optional.empty());
        given(redissonClient.<TimeSaleOrderStatus>getBucket(resultKey))
                .willReturn(resultBucket);
        given(redissonClient.<String>getScoredSortedSet(queueKey))
                .willReturn(queueBucket);
        given(redissonClient.getAtomicLong(totalKey))
                .willReturn(totalCounter);

        // when
        timeSaleConsumer.consumePurchaseRequest(message);

        // then
        verify(resultBucket).set(TimeSaleOrderStatus.FAIL);
        verify(totalCounter).decrementAndGet();
        verify(timeSaleOrderRepository, never()).save(any(TimeSaleOrder.class));
        verify(timeSaleRepository).findById(1L);
        verify(timeSaleRepository, never()).save(any(TimeSale.class));
    }

    @Test
    @DisplayName("구매 요청 처리 실패 - 재고 부족")
    void consumePurchaseRequest_OutOfStock() {
        // given
        TimeSale timeSaleWithLowStock = TimeSale.builder()
                .id(1L)
                .product(product)
                .status(TimeSaleStatus.ACTIVE)
                .quantity(100L)
                .remainingQuantity(1L)
                .discountPrice(5000L)
                .startAt(now.minusHours(1))
                .endAt(now.plusHours(1))
                .build();

        PurchaseRequestMessage message = PurchaseRequestMessage.builder()
                .requestId("test-request-id")
                .timeSaleId(1L)
                .userId(1L)
                .quantity(2L)
                .build();

        String resultKey = RESULT_PREFIX + message.getRequestId();
        String queueKey = QUEUE_KEY + message.getTimeSaleId();
        String totalKey = TOTAL_REQUESTS_KEY + message.getTimeSaleId();

        given(timeSaleRepository.findById(1L)).willReturn(Optional.of(timeSaleWithLowStock));
        given(redissonClient.<TimeSaleOrderStatus>getBucket(resultKey))
                .willReturn(resultBucket);
        given(redissonClient.<String>getScoredSortedSet(queueKey))
                .willReturn(queueBucket);
        given(redissonClient.getAtomicLong(totalKey))
                .willReturn(totalCounter);

        // when
        timeSaleConsumer.consumePurchaseRequest(message);

        // then
        verify(resultBucket).set(TimeSaleOrderStatus.FAIL);
        verify(totalCounter).decrementAndGet();
        verify(timeSaleOrderRepository, never()).save(any(TimeSaleOrder.class));
        verify(timeSaleRepository).findById(1L);
        verify(timeSaleRepository, never()).save(any(TimeSale.class));
    }

}