package com.bmcho.pointservice.exception;

import org.springframework.http.HttpStatus;

public class InsufficientPointBalanceException extends PointBasicException {
    public InsufficientPointBalanceException(Long balance, Long amount) {
        super(String.format("Insufficient point balance - balance: %d, amount: %d", balance, amount),
                HttpStatus.BAD_REQUEST);
    }

    public InsufficientPointBalanceException(String message, Long balance, Long amount) {
        super("%s - balance: %d, amount: %d".formatted(message, balance, amount),
                HttpStatus.FORBIDDEN);
    }
}
