package com.bmcho.couponservice.service.v1;

import com.bmcho.couponservice.aop.CouponMetered;
import com.bmcho.couponservice.config.interceptor.UserIdInterceptor;
import com.bmcho.couponservice.domain.Coupon;
import com.bmcho.couponservice.domain.CouponPolicy;
import com.bmcho.couponservice.dto.v1.CouponDto;
import com.bmcho.couponservice.exception.*;
import com.bmcho.couponservice.repository.CouponPolicyRepository;
import com.bmcho.couponservice.repository.CouponRepository;
import com.bmcho.couponservice.utll.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;

    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 1. Race Condition 발생 가능성
     * findByIdWithLock으로 쿠폰 정책에 대해 락을 걸지만, countByCouponPolicyId와 실제 쿠폰 저장 사이에 갭이 존재
     * 여러 트랜잭션이 동시에 카운트를 조회하고 조건을 통과한 후 쿠폰을 저장할 수 있음
     * 결과적으로 totalQuantity보다 더 많은 쿠폰이 발급될 수 있음
     * <p>
     * 2. 성능 이슈
     * 매 요청마다 발급된 쿠폰 수를 카운트하는 쿼리 실행
     * 쿠폰 수가 많아질수록 카운트 쿼리의 성능이 저하될 수 있음
     * PESSIMISTIC_LOCK으로 인한 병목 현상 발생 가능
     * <p>
     * 3. Dead Lock 가능성
     * 여러 트랜잭션이 동시에 같은 쿠폰 정책에 대해 락을 획득하려 할 때
     * 트랜잭션 타임아웃이 발생할 수 있음
     * <p>
     * 4. 정확한 수량 보장의 어려움
     * 분산 환경에서 여러 서버가 동시에 쿠폰을 발급할 경우
     * DB 레벨의 락만으로는 정확한 수량 제어가 어려움
     */
    @CouponMetered
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public Coupon issueCoupon(CouponDto.IssueRequest request) {
        CouponPolicy couponPolicy = couponPolicyRepository.findByIdWithLock(request.getCouponPolicyId())
                .orElseThrow(() -> new CouponPolicyNotFoundException(request.getCouponPolicyId()));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
            throw new CouponIssueNotAvailableException();
        }

        long issuedCouponCount = couponRepository.countByCouponPolicyId(couponPolicy.getId());
        if (issuedCouponCount >= couponPolicy.getTotalQuantity()) {
            throw new CouponOutOfStockException();
        }

        try {
            Coupon coupon = Coupon.builder()
                    .couponPolicy(couponPolicy)
                    .userId(UserIdInterceptor.getCurrentUserId())
                    .couponCode(generateCouponCode())
                    .build();

            couponRepository.save(coupon);
            log.info("Coupon issued successfully: policyId={}, userId={}", couponPolicy, UserIdInterceptor.getCurrentUserId());

            return coupon;
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (coupon_policy_id, user_id) 위반이면 → 이미 발급된 케이스 (멱등)
            if (Utils.isDuplicateKey(e)) {
                log.info("Coupon already issued (idempotent): policyId={}, userId={}",
                        couponPolicy.getId(), UserIdInterceptor.getCurrentUserId());

                throw new CouponAlreadyIssuedException(couponPolicy.getId(), UserIdInterceptor.getCurrentUserId());
            } else {
                log.error("Failed to issue coupon (DB error): {}", e.getMessage(), e);
                throw e; // 다른 DB 에러는 상위로 올려서 재시도/알람 대상
            }
        } catch (Exception e) {
            log.error("Failed to issue coupon: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        Coupon coupon = couponRepository.findByIdAndUserId(couponId, currentUserId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없거나 접근 권한이 없습니다."));

        coupon.use(orderId);

        return coupon;
    }

    @Transactional
    public Coupon cancelCoupon(Long couponId) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        Coupon coupon = couponRepository.findByIdAndUserId(couponId, currentUserId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없거나 접근 권한이 없습니다."));

        coupon.cancel();
        return coupon;
    }

    @Transactional(readOnly = true)
    public List<CouponDto.Response> getCoupons(CouponDto.ListRequest request) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        return couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        currentUserId,
                        request.getStatus(),
                        PageRequest.of(
                                request.getPage() != null ? request.getPage() : 0,
                                request.getSize() != null ? request.getSize() : 10
                        )
                ).stream()
                .map(CouponDto.Response::from)
                .toList();
    }
}