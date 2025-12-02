package com.bmcho.couponservice.service.v2;

import com.bmcho.couponservice.aop.CouponMetered;
import com.bmcho.couponservice.config.interceptor.UserIdInterceptor;
import com.bmcho.couponservice.domain.Coupon;
import com.bmcho.couponservice.domain.CouponPolicy;
import com.bmcho.couponservice.dto.v1.CouponDto;
import com.bmcho.couponservice.exception.CouponAlreadyIssuedException;
import com.bmcho.couponservice.exception.CouponIssueException;
import com.bmcho.couponservice.exception.CouponIssueNotAvailableException;
import com.bmcho.couponservice.exception.CouponIssueTooManyRequestsException;
import com.bmcho.couponservice.repository.CouponRepository;
import com.bmcho.couponservice.utll.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final CouponPolicyService couponPolicyService;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final long LOCK_WAIT_TIME = 3;
    private static final long LOCK_LEASE_TIME = 5;

    @Transactional
    @CouponMetered(version = "v2")
    public Coupon issueCoupon(CouponDto.IssueRequest request) {
        String quantityKey = COUPON_QUANTITY_KEY + request.getCouponPolicyId();
        String lockKey = COUPON_LOCK_KEY + request.getCouponPolicyId();
        RLock lock = redissonClient.getLock(lockKey);
        CouponPolicy couponPolicy = null;
        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new CouponIssueTooManyRequestsException();
            }

            couponPolicy = couponPolicyService.getCouponPolicy(request.getCouponPolicyId());

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new CouponIssueNotAvailableException();
            }

            // 수량 체크 및 감소
            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
            long remainingQuantity = atomicQuantity.decrementAndGet();

            if (remainingQuantity < 0) {
                atomicQuantity.incrementAndGet();
                throw new CouponAlreadyIssuedException(couponPolicy.getId(), UserIdInterceptor.getCurrentUserId());
            }

            // 쿠폰 발급
            return couponRepository.save(Coupon.builder()
                    .couponPolicy(couponPolicy)
                    .userId(UserIdInterceptor.getCurrentUserId())
                    .couponCode(generateCouponCode())
                    .build());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponIssueException();
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (coupon_policy_id, user_id) 위반이면 → 이미 발급된 케이스 (멱등)
            if (Utils.isDuplicateKey(e)) {
                log.info("Coupon already issued (idempotent): policyId={}, userId={}",
                        Objects.requireNonNull(couponPolicy).getId(), UserIdInterceptor.getCurrentUserId());

                throw new CouponAlreadyIssuedException(couponPolicy.getId(), UserIdInterceptor.getCurrentUserId());
            } else {
                log.error("Failed to issue coupon (DB error): {}", e.getMessage(), e);
                throw e; // 다른 DB 에러는 상위로 올려서 재시도/알람 대상
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String generateCouponCode() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}