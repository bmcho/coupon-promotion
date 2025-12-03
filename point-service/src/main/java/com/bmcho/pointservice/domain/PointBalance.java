package com.bmcho.pointservice.domain;

import com.bmcho.pointservice.exception.InsufficientPointBalanceException;
import com.bmcho.pointservice.exception.InvalidPointAmountException;
import com.bmcho.pointservice.exception.InvalidPointBalanceException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PointBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Long balance = 0L;  // 기본값 설정

    @Version
    private Long version = 0L;  // 기본값 설정

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public PointBalance(Long userId, Long balance) {
        this.userId = userId;
        this.balance = balance != null ? balance : 0L;  // null 체크 추가
        this.version = 0L;
    }

    public void addBalance(Long amount) {
        if (amount <= 0) {
            throw new InvalidPointAmountException(amount);
        }
        if (this.balance == null) {
            this.balance = 0L;
        }
        this.balance += amount;
    }

    public void subtractBalance(Long amount) {
        if (amount <= 0) {
            throw new InvalidPointAmountException(amount);
        }
        if (this.balance == null) {
            this.balance = 0L;
        }
        if (this.balance < amount) {
            throw new InsufficientPointBalanceException(this.balance, amount);
        }
        this.balance -= amount;
    }

    public void setBalance(Long balance) {
        if (balance == null || balance < 0) {
            throw new InvalidPointBalanceException(balance);
        }
        this.balance = balance;
    }
}