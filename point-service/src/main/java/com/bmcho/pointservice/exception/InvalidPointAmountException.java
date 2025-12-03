package com.bmcho.pointservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidPointAmountException extends PointBasicException {
    public InvalidPointAmountException(Long amount) {
        super(String.format("Amount must be positive - amount: %d", amount), HttpStatus.BAD_REQUEST);
    }
}