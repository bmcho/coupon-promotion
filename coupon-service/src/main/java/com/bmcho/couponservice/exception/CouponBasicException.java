package com.bmcho.couponservice.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class CouponBasicException extends RuntimeException {

    private final HttpStatus status;

    public CouponBasicException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public CouponBasicException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public CouponBasicException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
