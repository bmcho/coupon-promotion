package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;

public class CouponExpiredException extends CouponBasicException {
    public CouponExpiredException(String message) {
        super(message);
    }

    public CouponExpiredException(Long couponId) {
        super("만료된 쿠폰입니다: " + couponId, HttpStatus.CONFLICT);
    }
}