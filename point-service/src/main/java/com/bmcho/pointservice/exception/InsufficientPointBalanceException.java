package com.bmcho.pointservice.exception;

import org.springframework.http.HttpStatus;

public class InsufficientPointBalanceException extends PointBasicException {
    public InsufficientPointBalanceException(Long balance, Long amount) {
        super("Insufficient point balance - balance: %d, amount: %d".formatted(balance, amount),
                HttpStatus.BAD_REQUEST);
    }

    public InsufficientPointBalanceException(String message, Long balance, Long amount) {
        super("%s - balance: %d, amount: %d".formatted(message, balance, amount),
                HttpStatus.BAD_REQUEST);
    }
}
