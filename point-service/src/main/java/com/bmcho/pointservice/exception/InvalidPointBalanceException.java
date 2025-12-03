package com.bmcho.pointservice.exception;

public class InvalidPointBalanceException extends PointBasicException {
    public InvalidPointBalanceException(Long balance) {
        super(String.format("Balance cannot be negative or null - balance: %d", balance));
    }
}
