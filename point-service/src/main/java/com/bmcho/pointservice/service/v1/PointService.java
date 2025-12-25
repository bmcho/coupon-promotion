package com.bmcho.pointservice.service.v1;

import com.bmcho.pointservice.aop.PointMetered;
import com.bmcho.pointservice.domain.Point;
import com.bmcho.pointservice.domain.PointBalance;
import com.bmcho.pointservice.domain.PointType;
import com.bmcho.pointservice.exception.InsufficientPointBalanceException;
import com.bmcho.pointservice.exception.PointAlreadyCanceled;
import com.bmcho.pointservice.exception.PointNotFound;
import com.bmcho.pointservice.exception.UserNotFound;
import com.bmcho.pointservice.repository.PointBalanceRepository;
import com.bmcho.pointservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointBalanceRepository pointBalanceRepository;

    @PointMetered
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point earnPoints(Long userId, Long amount, String description) {
        PointBalance balance = pointBalanceRepository.findByUserId(userId)
                .orElseGet(() -> PointBalance.builder()
                        .userId(userId)
                        .balance(0L)
                        .build());

        balance.addBalance(amount);
        balance = pointBalanceRepository.save(balance);

        Point point = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.EARNED)
                .description(description)
                .balanceSnapshot(balance.getBalance())
                .pointBalance(balance)
                .build();

        return pointRepository.save(point);
    }

    @PointMetered
    @Transactional
    public Point usePoints(Long userId, Long amount, String description) {
        PointBalance balance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFound(userId));

        balance.subtractBalance(amount);
        balance = pointBalanceRepository.save(balance);

        Point point = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.USED)
                .description(description)
                .balanceSnapshot(balance.getBalance())
                .pointBalance(balance)
                .build();

        return pointRepository.save(point);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point cancelPoints(Long pointId, String description) {
        Point originalPoint = pointRepository.findById(pointId)
                .orElseThrow(() -> new PointNotFound(pointId));

        if (originalPoint.getType() == PointType.CANCELED) {
            throw new PointAlreadyCanceled(pointId);
        }

        PointBalance balance = pointBalanceRepository.findByUserId(originalPoint.getUserId())
                .orElseThrow(() -> new UserNotFound(originalPoint.getUserId()));

        Long currentBalance = balance.getBalance();
        Long newBalance;
        if (originalPoint.getType() == PointType.EARNED) {
            if (currentBalance < originalPoint.getAmount()) {
                throw new InsufficientPointBalanceException("Cannot cancel earned points by insufficient balance",
                        currentBalance, originalPoint.getAmount());
            }

            newBalance = currentBalance - originalPoint.getAmount();
        } else {
            newBalance = currentBalance + originalPoint.getAmount();
        }

        balance.setBalance(newBalance);
        balance = pointBalanceRepository.save(balance);

        Point cancelPoint = Point.builder()
                .userId(originalPoint.getUserId())
                .amount(originalPoint.getAmount())
                .type(PointType.CANCELED)
                .description(description)
                .balanceSnapshot(balance.getBalance())
                .pointBalance(balance)
                .build();

        return pointRepository.save(cancelPoint);
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance::getBalance)
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public Page<Point> getPointHistory(Long userId, Pageable pageable) {
        return pointRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
