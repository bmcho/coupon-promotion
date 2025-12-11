package com.bmcho.pointservice.service.v2;

import com.bmcho.pointservice.domain.Point;
import com.bmcho.pointservice.domain.PointBalance;
import com.bmcho.pointservice.domain.PointType;
import com.bmcho.pointservice.exception.InsufficientPointBalanceException;
import com.bmcho.pointservice.exception.PointAlreadyCanceled;
import com.bmcho.pointservice.repository.PointBalanceRepository;
import com.bmcho.pointservice.repository.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.useRepresentation;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointRedisServiceTest {

    @InjectMocks
    private PointRedisService pointRedisService;
    @Mock
    private PointRepository pointRepository;
    @Mock
    private PointBalanceRepository pointBalanceRepository;
    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private RMap<String, Long> rMap;

    private static final Long USER_ID = 1L;
    private static final Long POINT_ID = 1L;
    private static final Long AMOUNT = 1000L;
    private static final String DESCRIPTION = "Test description";


    @Test
    @DisplayName("포인트 적립 성공")
    void earnPointSuccess() throws InterruptedException {
        //given
        setupLockBehavior();
        setupMapBehavior();

        PointBalance pointBalance = PointBalance.builder()
                .userId(USER_ID)
                .balance(0L)
                .build();
        Point expectedPoint = Point.builder()
                .userId(USER_ID)
                .amount(AMOUNT)
                .type(PointType.EARNED)
                .description(DESCRIPTION)
                .balanceSnapshot(AMOUNT)
                .pointBalance(pointBalance)
                .build();

        given(pointBalanceRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point point = invocation.getArgument(0);
                    return Point.builder()
                            .userId(USER_ID)
                            .amount(point.getAmount())
                            .type(point.getType())
                            .description(point.getDescription())
                            .balanceSnapshot(point.getBalanceSnapshot())
                            .pointBalance(point.getPointBalance())
                            .build();
                });

        //when
        Point result = pointRedisService.earnPoints(USER_ID, AMOUNT, DESCRIPTION);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(expectedPoint.getAmount());
        assertThat(result.getType()).isEqualTo(expectedPoint.getType());
        assertThat(result.getDescription()).isEqualTo(expectedPoint.getDescription());
        verify(rMap).fastPut(eq(USER_ID.toString()), eq(AMOUNT));

    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePointSuccess() throws InterruptedException {
        //given
        setupLockBehavior();
        setupMapBehavior();

        PointBalance pointBalance = PointBalance.builder()
                .userId(USER_ID)
                .balance(AMOUNT)
                .build();
        Point expectedPoint = Point.builder()
                .userId(USER_ID)
                .amount(AMOUNT)
                .type(PointType.USED)
                .description(DESCRIPTION)
                .balanceSnapshot(0L)
                .pointBalance(pointBalance)
                .build();

        given(rMap.get(USER_ID.toString())).willReturn(AMOUNT);
        given(pointBalanceRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point point = invocation.getArgument(0);
                    return Point.builder()
                            .userId(USER_ID)
                            .amount(point.getAmount())
                            .type(point.getType())
                            .description(point.getDescription())
                            .balanceSnapshot(point.getBalanceSnapshot())
                            .pointBalance(point.getPointBalance())
                            .build();
                });

        Point result = pointRedisService.usePoints(USER_ID, AMOUNT, DESCRIPTION);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(expectedPoint.getAmount());
        assertThat(result.getType()).isEqualTo(expectedPoint.getType());
        assertThat(result.getDescription()).isEqualTo(expectedPoint.getDescription());
        assertThat(result.getBalanceSnapshot()).isEqualTo(expectedPoint.getBalanceSnapshot());
        verify(rMap).fastPut(eq(USER_ID.toString()), eq(0L));
    }

    @Test
    @DisplayName("포인트 사용 실패 - 포인트 부족")
    void usePointFailureByInsufficientPoint() throws InterruptedException {
        setupLockBehavior();
        setupMapBehavior();

        PointBalance pointBalance = PointBalance.builder()
                .userId(USER_ID)
                .balance(10L)
                .build();

        given(rMap.get(USER_ID.toString())).willReturn(10L);

        assertThatThrownBy(() -> pointRedisService.usePoints(USER_ID, AMOUNT, DESCRIPTION))
                .isInstanceOf(InsufficientPointBalanceException.class)
                .hasMessage("Insufficient point balance - balance: %d, amount: %d".formatted(10L, AMOUNT));

    }

    @Test
    @DisplayName("포인트 취소 성공")
    void cancelPointSuccess() throws InterruptedException {
        setupLockBehavior();
        setupMapBehavior();

        PointBalance pointBalance = PointBalance.builder()
                .userId(USER_ID)
                .balance(AMOUNT)
                .build();
        Point originalPoint = Point.builder()
                .userId(USER_ID)
                .amount(AMOUNT)
                .type(PointType.EARNED)
                .pointBalance(pointBalance)
                .build();
        Point expectedPoint = Point.builder()
                .userId(USER_ID)
                .amount(AMOUNT)
                .type(PointType.CANCELED)
                .description(DESCRIPTION)
                .balanceSnapshot(0L)
                .pointBalance(pointBalance)
                .build();

        given(pointRepository.findById(POINT_ID))
                .willReturn(Optional.of(originalPoint));

        given(pointBalanceRepository.findByUserId(originalPoint.getUserId()))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point point = invocation.getArgument(0);
                    return Point.builder()
                            .userId(USER_ID)
                            .amount(point.getAmount())
                            .type(point.getType())
                            .description(point.getDescription())
                            .balanceSnapshot(point.getBalanceSnapshot())
                            .pointBalance(point.getPointBalance())
                            .build();
                });

        Point result = pointRedisService.cancelPoints(POINT_ID, DESCRIPTION);
        // then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(PointType.CANCELED);
        verify(rMap).fastPut(eq(USER_ID.toString()), eq(0L));

    }

    @Test
    @DisplayName("포인트 취소 실패 - 이미 취소된 포인트")
    void cancelPointFailureByAlreadyCanceled() throws InterruptedException {
        setupLockBehavior();

        Point originalPoint = Point.builder()
                .userId(USER_ID)
                .amount(AMOUNT)
                .type(PointType.CANCELED)
                .build();

        given(pointRepository.findById(POINT_ID))
                .willReturn(Optional.of(originalPoint));


        assertThatThrownBy(() -> pointRedisService.cancelPoints(POINT_ID, DESCRIPTION))
                .isInstanceOf(PointAlreadyCanceled.class)
                .hasMessage("Point Already Canceled. pointId : %d".formatted(POINT_ID));


    }

    @Test
    @DisplayName("redis 잔액 조회 성공")
    void getBalanceFromCache() {
        // given
        setupMapBehavior();
        given(rMap.get(USER_ID.toString())).willReturn(AMOUNT);

        // when
        Long balance = pointRedisService.getBalance(USER_ID);

        // then
        assertThat(balance).isEqualTo(AMOUNT);
        verify(pointBalanceRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("DB 잔액 조회 후 캐시 업데이트")
    void getBalanceFromDB() {
        // given
        setupMapBehavior();
        PointBalance pointBalance = PointBalance.builder()
                .userId(USER_ID)
                .balance(AMOUNT)
                .build();

        given(rMap.get(USER_ID.toString())).willReturn(null);
        given(pointBalanceRepository.findByUserId(USER_ID)).willReturn(Optional.of(pointBalance));

        // when
        Long balance = pointRedisService.getBalance(USER_ID);

        // then
        assertThat(balance).isEqualTo(AMOUNT);
        verify(rMap).fastPut(eq(USER_ID.toString()), eq(AMOUNT));
    }

    private void setupLockBehavior() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
    }

    private void setupMapBehavior() {
        given(redissonClient.<String, Long>getMap(anyString())).willReturn(rMap);
    }

}