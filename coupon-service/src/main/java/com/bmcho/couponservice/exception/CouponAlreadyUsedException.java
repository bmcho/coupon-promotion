package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;

public class CouponAlreadyUsedException extends CouponBasicException {

    public CouponAlreadyUsedException(String message) {
        super(message);
    }

    public CouponAlreadyUsedException(Long couponId) {
        super("이미 사용된 쿠폰입니다: " + couponId, HttpStatus.CONFLICT);
    }
}