package com.bmcho.pointservice.service.v1;

import com.bmcho.pointservice.domain.Point;
import com.bmcho.pointservice.domain.PointBalance;
import com.bmcho.pointservice.domain.PointType;
import com.bmcho.pointservice.exception.InsufficientPointBalanceException;
import com.bmcho.pointservice.exception.PointAlreadyCanceled;
import com.bmcho.pointservice.exception.PointNotFound;
import com.bmcho.pointservice.exception.UserNotFound;
import com.bmcho.pointservice.repository.PointBalanceRepository;
import com.bmcho.pointservice.repository.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointBalanceRepository pointBalanceRepository;


    private Long userId;
    private Long amount;
    private String description;
    private PointBalance pointBalance;
    private Point point;

    @BeforeEach
    void setUp() {
        userId = 1L;
        amount = 1000L;
        description = "Test points";

        pointBalance = PointBalance.builder()
                .userId(userId)
                .balance(1000L) // 초기 잔액을 1000으로 설정
                .build();

        point = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.EARNED)
                .description(description)
                .balanceSnapshot(amount)
                .build();
    }

    @Test
    @DisplayName("포인트 적립 성공")
    void earnPointsSuccess() {

        //given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        //when
        Point result = pointService.earnPoints(userId, amount, description);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));

    }

    @Test
    @DisplayName("새로운 사용자의 포인트 적립 성공 테스트")
    void earnPointsNewUserSuccess() {

        //given

        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.empty());
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        //when
        Point result = pointService.earnPoints(userId, amount, description);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 사용 성공 테스트")
    void usePointsSuccess() {
        //given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when
        Point result = pointService.usePoints(userId, amount, description);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.USED);

        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));

    }

    @Test
    @DisplayName("잔액 부족으로 포인트 사용 실패 테스트")
    void usePointsInsufficientBalance() {

        //given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));

        // when & then
        assertThatThrownBy(() -> pointService.usePoints(userId, amount * 2, description))
                .isInstanceOf(InsufficientPointBalanceException.class)
                .hasMessage("Insufficient point balance - balance: %d, amount: %d"
                        .formatted(pointBalance.getBalance(), amount * 2));
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 포인트 사용 시도 테스트")
    void usePointsUserNotFound() {
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.usePoints(userId, amount, description))
                .isInstanceOf(UserNotFound.class)
                .hasMessage("User Not Found. userId : %d".formatted(userId));
    }

    @Test
    @DisplayName("포인트 적립 취소 성공 테스트")
    void cancelEarnedPointsSuccess() {
        Point basePoint = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.EARNED)
                .description("Earned point cancel")
                .balanceSnapshot(1000L)
                .pointBalance(pointBalance)
                .build();

        given(pointRepository.findById(1L))
                .willReturn(Optional.of(basePoint));
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        //when
        String msg = "Earned point cancel Test";
        Point result = pointService.cancelPoints(1L, msg);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(PointType.CANCELED);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getDescription()).isEqualTo(msg);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 사용 취소 성공 테스트")
    void cancelSpentPointsSuccess() {
        // given
        Point basePoint = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.USED)
                .description("Used point")
                .balanceSnapshot(1000L)
                .pointBalance(pointBalance)
                .build();

        given(pointRepository.findById(1L))
                .willReturn(Optional.of(basePoint));
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(pointBalance));
        given(pointBalanceRepository.save(any(PointBalance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(pointRepository.save(any(Point.class)))
                .willAnswer(invocation -> {
                    Point savedPoint = invocation.getArgument(0);
                    return Point.builder()
                            .userId(savedPoint.getUserId())
                            .amount(savedPoint.getAmount())
                            .type(savedPoint.getType())
                            .description(savedPoint.getDescription())
                            .balanceSnapshot(savedPoint.getBalanceSnapshot())
                            .pointBalance(savedPoint.getPointBalance())
                            .build();
                });

        // when

        String msg = "Used point cancel Test";
        Point result = pointService.cancelPoints(1L, msg);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(PointType.CANCELED);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getDescription()).isEqualTo(msg);
        verify(pointBalanceRepository, times(1)).save(any(PointBalance.class));
        verify(pointRepository, times(1)).save(any(Point.class));

    }

    @Test
    @DisplayName("이미 취소된 포인트 취소 시도 테스트")
    void cancelAlreadyCanceledPoints() {
        // given
        Point canceledPoint = Point.builder()
                .userId(userId)
                .amount(amount)
                .type(PointType.CANCELED)
                .description("Already canceled")
                .balanceSnapshot(0L)
                .pointBalance(pointBalance)
                .build();

        given(pointRepository.findById(1L))
                .willReturn(Optional.of(canceledPoint));

        // when & then
        assertThatThrownBy(() -> pointService.cancelPoints(1L, "Cancel test"))
                .isInstanceOf(PointAlreadyCanceled.class)
                .hasMessage("Point Already Canceled. pointId : %d".formatted(1L));
    }

    @Test
    @DisplayName("존재하지 않는 포인트 취소 시도 테스트")
    void cancelNonExistentPoints() {
        // given
        given(pointRepository.findById(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.cancelPoints(1L, "Cancel test"))
                .isInstanceOf(PointNotFound.class)
                .hasMessage("Point Not Found. pointId : %d".formatted(1L));
    }

    @Test
    @DisplayName("포인트 잔액 조회 테스트")
    void getBalanceSuccess() {
        // given
        PointBalance balance = PointBalance.builder()
                .userId(userId)
                .balance(1000L)
                .build();

        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.of(balance));

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isEqualTo(1000L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 포인트 잔액 조회 테스트")
    void getBalanceUserNotFound() {
        // given
        given(pointBalanceRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isZero();
    }

}