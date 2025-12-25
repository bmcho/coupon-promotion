package com.bmcho.pointservice.service.v2;

import com.bmcho.pointservice.aop.PointMetered;
import com.bmcho.pointservice.domain.Point;
import com.bmcho.pointservice.domain.PointBalance;
import com.bmcho.pointservice.domain.PointType;
import com.bmcho.pointservice.exception.*;
import com.bmcho.pointservice.repository.PointBalanceRepository;
import com.bmcho.pointservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;


/**
 * Redis를 활용한 포인트 서비스 V2 구현
 * - Redisson 분산 락을 통한 동시성 제어
 * - Redis 캐시를 통한 성능 최적화
 */
@Service
@RequiredArgsConstructor
public class PointRedisService {

    private static final String POINT_BALANCE_MAP = "point:balance";
    private static final String POINT_LOCK_PREFIX = "point:lock";
    private static final Long LOCK_WAIT_TIME = 3L;
    private static final Long LOCK_LEASE_TIME = 3L;

    private final PointRepository pointRepository;
    private final PointBalanceRepository pointBalanceRepository;
    private final RedissonClient redissonClient;

    /**
     * 포인트 적립 처리
     * 1. 분산 락 획득
     * 2. 캐시된 잔액 조회 (없으면 DB에서 조회)
     * 3. 포인트 잔액 증가
     * 4. DB 저장 및 캐시 업데이트
     * 5. 포인트 이력 저장
     */
    @PointMetered("v2")
    @Transactional
    public Point earnPoints(Long userId, Long amount, String description) {
        //분산락
        String lockKey = POINT_LOCK_PREFIX + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new LockAcquisitionFailedException(lockKey);
            }

            Long currentBalance = getBalance(userId);

            // 포인트 잔액 증가
            PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                    .orElseGet(() -> PointBalance.builder()
                            .userId(userId)
                            .balance(0L)
                            .build());

            pointBalance.addBalance(amount);
            pointBalance = pointBalanceRepository.save(pointBalance);
            updateBalanceCache(userId, pointBalance.getBalance());

            Point point = Point.builder()
                    .userId(userId)
                    .amount(amount)
                    .type(PointType.EARNED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();

            return pointRepository.save(point);

        } catch (InterruptedException e) {
            // 락 획득 중 인터럽트 발생
            Thread.currentThread().interrupt();
            throw new PointBasicException("Lock acquisition was interrupted", e.getCause());
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 포인트 사용 처리
     * 1. 분산 락 획득
     * 2. 캐시된 잔액 조회 (없으면 DB에서 조회)
     * 3. 잔액 체크
     * 4. 포인트 잔액 감소
     * 5. DB 저장 및 캐시 업데이트
     * 6. 포인트 이력 저장
     */
    @PointMetered("v2")
    @Transactional
    public Point usePoints(Long userId, Long amount, String description) {
        String lockKey = POINT_LOCK_PREFIX + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new LockAcquisitionFailedException(lockKey);
            }

            Long currentBalance = getBalance(userId);

            if (currentBalance < amount) {
                throw new InsufficientPointBalanceException(currentBalance, amount);
            }

            PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                    .orElseThrow(() -> new UserNotFound(userId));

            pointBalance.subtractBalance(amount);
            pointBalance = pointBalanceRepository.save(pointBalance);
            updateBalanceCache(userId, pointBalance.getBalance());

            Point point = Point.builder()
                    .userId(userId)
                    .amount(amount)
                    .type(PointType.USED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();

            return pointRepository.save(point);

        } catch (InterruptedException e) {
            // 락 획득 중 인터럽트 발생
            Thread.currentThread().interrupt();
            throw new PointBasicException("Lock acquisition was interrupted", e.getCause());
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 포인트 취소 처리
     * 1. 원본 포인트 이력 조회
     * 2. 분산 락 획득
     * 3. 취소 가능 여부 확인
     * 4. 포인트 잔액 원복 (적립 취소는 차감, 사용 취소는 증가)
     * 5. DB 저장 및 캐시 업데이트
     * 6. 취소 이력 저장
     */
    @Transactional
    public Point cancelPoints(Long pointId, String description) {
        Point originalPoint = pointRepository.findById(pointId)
                .orElseThrow(() -> new PointNotFound(pointId));

        Long userId = originalPoint.getUserId();
        String lockKey = POINT_LOCK_PREFIX + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new LockAcquisitionFailedException(lockKey);
            }

            if (originalPoint.getType() == PointType.CANCELED) {
                throw new PointAlreadyCanceled(pointId);
            }

            PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                    .orElseThrow(() -> new UserNotFound(userId));

            if (originalPoint.getType() == PointType.EARNED) {
                pointBalance.subtractBalance(originalPoint.getAmount());
            } else {
                pointBalance.addBalance(originalPoint.getAmount());
            }

            pointBalance = pointBalanceRepository.save(pointBalance);
            updateBalanceCache(userId, pointBalance.getBalance());

            Point point = Point.builder()
                    .userId(userId)
                    .amount(originalPoint.getAmount())
                    .type(PointType.CANCELED)
                    .description(description)
                    .balanceSnapshot(pointBalance.getBalance())
                    .pointBalance(pointBalance)
                    .build();

            return pointRepository.save(point);

        } catch (InterruptedException e) {
            // 락 획득 중 인터럽트 발생
            Thread.currentThread().interrupt();
            throw new PointBasicException("Lock acquisition was interrupted", e.getCause());
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        Long cachedBalance = getBalanceFromCache(userId);
        if (cachedBalance != null) {
            return cachedBalance;
        }

        Long dbBalance = getBalanceFromDB(userId);
        updateBalanceCache(userId, dbBalance);
        return dbBalance;
    }

    /**
     * Redis 캐시에서 잔액 조회
     */
    private Long getBalanceFromCache(Long userId) {
        RMap<String, Long> balanceMap = redissonClient.getMap(POINT_BALANCE_MAP);
        return balanceMap.get(String.valueOf(userId));
    }

    private void updateBalanceCache(Long userId, Long currentBalance) {
        RMap<String, Long> balanceMap = redissonClient.getMap(POINT_BALANCE_MAP);
        balanceMap.fastPut(String.valueOf(userId), currentBalance);
    }

    private Long getBalanceFromDB(Long userId) {
        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance::getBalance)
                .orElse(0L);
    }


}
