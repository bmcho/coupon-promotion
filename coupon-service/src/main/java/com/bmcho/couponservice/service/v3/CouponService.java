package com.bmcho.couponservice.service.v3;

import com.bmcho.couponservice.aop.CouponMetered;
import com.bmcho.couponservice.config.interceptor.UserIdInterceptor;
import com.bmcho.couponservice.domain.Coupon;
import com.bmcho.couponservice.domain.CouponPolicy;
import com.bmcho.couponservice.dto.v3.CouponDto;
import com.bmcho.couponservice.exception.*;
import com.bmcho.couponservice.repository.CouponRepository;
import com.bmcho.couponservice.service.v2.CouponPolicyService;
import com.bmcho.couponservice.service.v2.CouponStateService;
import com.bmcho.couponservice.utll.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("couponServiceV3")
@RequiredArgsConstructor
public class CouponService {

    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final CouponPolicyService couponPolicyService;
    private final CouponStateService couponStateService;
    private final CouponProducer couponProducer;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final long LOCK_WAIT_TIME = 3;
    private static final long LOCK_LEASE_TIME = 5;

    @Transactional(readOnly = true)
    public void requestCouponIssue(CouponDto.IssueRequest request) {
        String quantityKey = COUPON_QUANTITY_KEY + request.getCouponPolicyId();
        String lockKey = COUPON_LOCK_KEY + request.getCouponPolicyId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new CouponIssueTooManyRequestsException();
            }

            CouponPolicy couponPolicy = couponPolicyService.getCouponPolicy(request.getCouponPolicyId());
            if (couponPolicy == null) {
                throw new CouponPolicyNotFoundException(request.getCouponPolicyId());
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new CouponIssueNotAvailableException();
            }

            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
            long remainingQuantity = atomicQuantity.decrementAndGet();

            if (remainingQuantity < 0) {
                atomicQuantity.incrementAndGet();
                throw new CouponOutOfStockException();
            }

            couponProducer.sendCouponIssueRequest(
                    CouponDto.IssueMessage.builder()
                            .policyId(request.getCouponPolicyId())
                            .userId(UserIdInterceptor.getCurrentUserId())
                            .build()
            );


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponIssueException();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @CouponMetered(version = "v3")
    @Transactional
    public void issueCoupon(CouponDto.IssueMessage message) {
        try {
            CouponPolicy policy = couponPolicyService.getCouponPolicy(message.getPolicyId());
            if (policy == null) {
                throw new CouponPolicyNotFoundException(message.getPolicyId());
            }
            Coupon coupon = Coupon.builder()
                    .couponPolicy(policy)
                    .userId(message.getUserId())
                    .couponCode(generateCouponCode()) // 병민님이 이미 가진 util 메서드
                    .build();

            couponRepository.save(coupon);

            log.info("Coupon issued successfully: policyId={}, userId={}", message.getPolicyId(), message.getUserId());

        } catch (DataIntegrityViolationException e) {
            // UNIQUE (coupon_policy_id, user_id) 위반이면 → 이미 발급된 케이스 (멱등)
            if (Utils.isDuplicateKey(e)) {
                log.info("Coupon already issued (idempotent): policyId={}, userId={}",
                        message.getPolicyId(), message.getUserId());

                throw new CouponAlreadyIssuedException(message.getPolicyId(), message.getUserId());
                // 예외를 다시 던지지 않음 → 정상 처리로 간주
            } else {
                log.error("Failed to issue coupon (DB error): {}", e.getMessage(), e);
                throw new CouponIssueException(e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to issue coupon: {}", e.getMessage());
            throw new CouponIssueException(e.getMessage());
        }
    }

    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId) {
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        coupon.use(orderId);
        couponStateService.updateCouponState(coupon);

        return coupon;
    }

    @Transactional
    public Coupon cancelCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdAndUserId(couponId, UserIdInterceptor.getCurrentUserId())
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        if (!coupon.isUsed()) {
            throw new CouponIssueException("사용되지 않은 쿠폰은 취소할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        coupon.cancel();
        couponStateService.updateCouponState(coupon);

        return coupon;
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

}
