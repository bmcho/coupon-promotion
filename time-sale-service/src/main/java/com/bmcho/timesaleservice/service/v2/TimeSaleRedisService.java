package com.bmcho.timesaleservice.service.v2;

import com.bmcho.timesaleservice.domain.Product;
import com.bmcho.timesaleservice.domain.TimeSale;
import com.bmcho.timesaleservice.domain.TimeSaleOrder;
import com.bmcho.timesaleservice.domain.TimeSaleStatus;
import com.bmcho.timesaleservice.dto.TimeSaleDto;
import com.bmcho.timesaleservice.exception.ProductException;
import com.bmcho.timesaleservice.exception.TimeSaleBasicException;
import com.bmcho.timesaleservice.exception.TimeSaleException;
import com.bmcho.timesaleservice.exception.common.ErrorCode;
import com.bmcho.timesaleservice.repository.ProductRepository;
import com.bmcho.timesaleservice.repository.TimeSaleOrderRepository;
import com.bmcho.timesaleservice.repository.TimeSaleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSaleRedisService {
    private static final String TIME_SALE_KEY = "time-sale:";
    private static final String TIME_SALE_LOCK = "time-sale-lock:";
    private static final long WAIT_TIME = 3L;
    private static final long LEASE_TIME = 3L;

    private final TimeSaleRepository timeSaleRepository;
    private final ProductRepository productRepository;
    private final TimeSaleOrderRepository timeSaleOrderRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public TimeSale createTimeSale(TimeSaleDto.CreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ProductException.notFound(request.getProductId()));

        TimeSale timeSale = TimeSale.builder()
                .product(product)
                .quantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .discountPrice(request.getDiscountPrice())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(TimeSaleStatus.ACTIVE)
                .build();

        TimeSale savedTimeSale = timeSaleRepository.save(timeSale);
        saveToRedis(savedTimeSale);
        return savedTimeSale;
    }

    @Transactional(readOnly = true)
    public Page<TimeSale> getOngoingTimeSales(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return timeSaleRepository.findAllByStartAtBeforeAndEndAtAfterAndStatus(
                now, TimeSaleStatus.ACTIVE, pageable);
    }


    @Transactional(readOnly = true)
    public TimeSale getTimeSale(Long timeSaleId) {
        return getFromRedis(timeSaleId);
    }

    @Transactional
    public TimeSale purchaseTimeSale(Long timeSaleId, TimeSaleDto.PurchaseRequest request) {
        RLock lock = redissonClient.getLock(TIME_SALE_LOCK + timeSaleId);
        if (lock == null) {
            throw TimeSaleException.failedToCreateRedisLock(timeSaleId);
        }

        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw TimeSaleException.failedToAcquireRedisLock();
            }

            TimeSale timeSale = getFromRedis(timeSaleId);
            timeSale.purchase(request.getQuantity());

            // Save changes to DB
            timeSale = timeSaleRepository.save(timeSale);

            TimeSaleOrder order = TimeSaleOrder.builder()
                    .userId(request.getUserId())
                    .timeSale(timeSale)
                    .quantity(request.getQuantity())
                    .discountPrice(timeSale.getDiscountPrice())
                    .build();

            timeSaleOrderRepository.save(order);
            saveToRedis(timeSale);

            return timeSale;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("TimeSale purchase time sale's lock has been interrupted: {}", TIME_SALE_LOCK + timeSaleId, e);
            throw new TimeSaleException(
                    ErrorCode.UNHANDLED_SERVER_ERROR,
                    "imeSale purchase time sale's lock has been interrupted",
                    null);
        } finally {
            if (isLocked) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("Failed to unlock", e);
                }
            }
        }
    }

    public void saveToRedis(TimeSale timeSale) {
        try {
            String json = objectMapper.writeValueAsString(timeSale);
            RBucket<String> bucket = redissonClient.getBucket(TIME_SALE_KEY + timeSale.getId());
            bucket.set(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TimeSale: {}", timeSale.getId(), e);
        } catch (Exception e) {
            log.error("Failed to save TimeSale to Redis: {}", timeSale.getId(), e);
        }
    }

    private TimeSale getFromRedis(Long timeSaleId) {
        RBucket<String> bucket = redissonClient.getBucket(TIME_SALE_KEY + timeSaleId);
        String json = bucket.get();

        try {
            if (json != null) {
                return objectMapper.readValue(json, TimeSale.class);
            }

            // Redis에 없으면 DB에서 조회
            TimeSale timeSale = timeSaleRepository.findById(timeSaleId)
                    .orElseThrow(() -> TimeSaleException.notFound(timeSaleId));

            // Redis에 저장
            saveToRedis(timeSale);

            return timeSale;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TimeSale to Redis: {}", timeSaleId, e);
            throw new TimeSaleException(
                    ErrorCode.UNHANDLED_SERVER_ERROR,
                    "Failed to parse TimeSale from Redis",
                    null
            );
        }
    }
}