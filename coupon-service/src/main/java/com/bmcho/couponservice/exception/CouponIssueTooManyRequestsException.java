package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;

public class CouponIssueTooManyRequestsException extends CouponBasicException {

    public CouponIssueTooManyRequestsException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public CouponIssueTooManyRequestsException() {
        super("쿠폰 발급 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS);
    }
}